package com.aajpm.altair.utility.solver;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import com.aajpm.altair.utility.exception.BodyNotFoundException;
import com.aajpm.altair.utility.exception.SolverException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

public class HorizonsEphemeridesSolver extends EphemeridesSolver {

    private WebClient client;
    private Map<String, String> baseParams;

    public HorizonsEphemeridesSolver(double latitude, double longitude, double elevation) {
        super(latitude, longitude, elevation);
        client = WebClient.builder()
                .baseUrl("https://ssd.jpl.nasa.gov/api/")
                .build();

        // Set the base parameters for the calls
        baseParams = new HashMap<>(10);
        baseParams.put("MAKE_EPHEM", "YES");        // Calc ephemerides
        baseParams.put("OBJ_DATA", "NO");           // Don't include object data
        baseParams.put("EPHEM_TYPE", "OBSERVER");   // Select observer ephemerides
        baseParams.put("CENTER", "coord@399");      // Center on the observatory
        baseParams.put("SITE_COORD", String.format("%f,%f,%f", longitude, latitude, elevation / 1000)); // Set the observatory coordinates
        baseParams.put("APPARENT", "REFRACTED");    // Account for atmospheric refraction
        baseParams.put("EXTRA_PREC", "YES");        // Use extra precision for coordinates
        baseParams.put("CSV_FORMAT", "YES");        // Add commas to separate values
    }

    @Override
    public Mono<double[]> getAltAz(String body, Instant time) {
        Map<String, String> params = new HashMap<>(baseParams);
        
        // Fix the time string to be in the correct format for Horizons
        String timeString = time.toString().replace("T", " ").replace("Z", "");
        params.put("TLIST", timeString);
        params.put("QUANTITIES", "4"); // Alt/Az
        
        return getBodyId(body).flatMap(id -> {
            params.put("COMMAND", "" + id);
            return getReq(params).flatMap(response -> {
                String[] lines = response.split("\n");
                
                // Scroll through the lines until it finds the start of the data
                int i = 0;
                while (i < lines.length && !lines[i].contains("$$SOE")) {
                    i++;
                }

                if (i >= lines.length) {
                    return Mono.error(new SolverException("Horizons returned an unexpected response for " + body));
                }

                if (lines[i + 1].contains("No ephemeris")) {
                    return Mono.error(new SolverException("Horizons returned no ephemeris for " + body + " at " + timeString));
                }

                String[] data = lines[i + 1].trim().split(",");
                double az = Double.parseDouble(data[3]);
                double alt = Double.parseDouble(data[4]);

                return Mono.just(new double[] {alt, az});
            });
        });
    }

    @Override
    @SuppressWarnings("java:S3776")
    public Mono<Boolean> isVisible(String body, Instant time, double altitude) {
        
        Map<String, String> params = new HashMap<>(baseParams);
        
        // Fix the time string to be in the correct format for Horizons
        String timeString = time.toString().replace("T", " ").replace("Z", "");
        params.put("TLIST", timeString);
        params.put("QUANTITIES", "4,12"); // Alt/Az + visibility
        
        if (altitude > 0) {
            params.put("ELEV_CUT", "" + altitude);
        }
        
        return getBodyId(body).flatMap(id -> {
            params.put("COMMAND", "" + id);
            return getReq(params).flatMap(response -> {
                String[] lines = response.split("\n");
                
                // Scroll through the lines until it finds the start of the data
                int i = 0;
                while (i < lines.length && !lines[i].contains("$$SOE")) {
                    i++;
                }

                if (i >= lines.length) {
                    return Mono.error(new SolverException("Horizons returned an unexpected response for " + body));
                }

                if (lines[i + 1].contains("No ephemeris")) {
                    return Mono.error(new SolverException("Horizons returned no ephemeris for " + body + " at " + timeString));
                }


                String[] data = lines[i + 1].trim().split(",");

                if (id.equals("" + 10) || id.equals("" + 301)) {    // If Sun or Moon, just check altitude, they can be seen in the day
                    boolean isAboveHorizon = Double.parseDouble(data[4]) > altitude;
                    return Mono.just(isAboveHorizon);
                }

                boolean isNight = data[1].isBlank();                       // blank means night OR geocentric ephemeris
                boolean visible = data[6].trim().startsWith("*");   // * means free and clear  


                return (id.equals("" + 10) || id.equals("" + 301)) ?  Mono.just(visible) : Mono.just(isNight && visible);
            });
        });
    }

    @Override
    @SuppressWarnings("java:S3776")
    public Mono<Instant[]> getRiseSetTimes(String body, Instant baseTime, Duration howFar, double altitude) {
        
        Map<String, String> params = new HashMap<>(baseParams);

        // Fix the time string to be in the correct format for Horizons
        String baseTimeString = baseTime.toString().replace("T", " ").replace("Z", "");
        params.put("START_TIME", baseTimeString);
        
        Instant endTime = baseTime.plus(howFar);
        String endTimeString = endTime.toString().replace("T", " ").replace("Z", "");
        params.put("STOP_TIME", endTimeString);

        params.put("STEP_SIZE", "1m TVH");  // Precision of 1 minute, highest precision available

        if (altitude != 0) {
            params.put("ELEV_CUT", "" + altitude);
        }

        params.put("QUANTITIES", "4,12"); // Alt/Az + visibility

        
        return getBodyId(body).flatMap(id -> {
            params.put("COMMAND", "" + id);
            return getReq(params).flatMap(response -> {
                String[] lines = response.split("\n");
                
                // Scroll through the lines until it finds the start and the end of the data
                int dataStart;
                int dataEnd;
                int i = 0;

                while (i < lines.length && !lines[i].contains("$$SOE")) {
                    i++;
                }

                dataStart = i;

                if (dataStart >= lines.length) {
                    return Mono.error(new SolverException("Horizons returned an unexpected response for " + body));
                } else if (lines[++i].contains("No ephemeris")) {
                    return Mono.error(new SolverException("Horizons returned no ephemeris for " + body + " from " + baseTimeString + " to " + endTimeString));
                }

                while (i < lines.length && !lines[i].contains("$$EOE")) {
                    i++;
                }

                dataEnd = i;

                if (dataEnd == (dataStart + 1)) {
                    return Mono.error(new SolverException("Horizons returned an unexpected response for " + body));
                }

                Instant riseTime = null;
                Instant setTime = null;

                if (!lines[dataStart+1].split(",")[2].trim().equals(MARKER_RISE)) {
                    riseTime = baseTime;
                }

                i = dataStart + 1;

                // Find the first rise and set times
                while (i < dataEnd && (riseTime == null || setTime == null)) {
                    String[] data = lines[i].trim().split(",");

                    if (data[2].trim().equals(MARKER_RISE) && riseTime == null) {
                        riseTime = parseHorizonsDate(data[0].trim());
                    } else if (data[2].trim().equals(MARKER_SET) && setTime == null) {
                        setTime = parseHorizonsDate(data[0].trim());
                    }
                    
                    i++;
                }

                if (riseTime == null) {
                    return Mono.error(new SolverException("Horizons returned no ephemeris for " + body + " from " + baseTimeString + " to " + endTimeString));
                }

                return Mono.just(new Instant[] {riseTime, setTime});
            });
        });
    }

    @Override
    public Mono<Double> getLST(Instant time, double longitude, boolean useHoursInstead) {
        return Mono.just(getLSTImpl(time, longitude, useHoursInstead));
    }

    protected double getLSTImpl(Instant time, double longitude, boolean useHoursInstead) {
        // Using the formula from http://www.stargazing.net/kepler/altaz.html
        double daysSinceJ2000 = getJ2000Time(time).toSeconds() / 86400.0;
        ZonedDateTime utcTime = time.atZone(ZoneOffset.UTC);
        double utc = utcTime.getHour() + utcTime.getMinute() / 60.0 + utcTime.getSecond() / 3600.0; 
        double lst = (100.46 + 0.985647 * daysSinceJ2000 + longitude + 15 * utc) % 360;
        
        return useHoursInstead ? lst / 15.0 : lst;
    }
    
    @Override
    public Mono<double[]> raDecToAltAz(double ra, double dec, double latitude, double longitude, Instant time) {
        return Mono.just(raDecToAltAzImpl(ra, dec, latitude, longitude, time));
    }

    protected double[] raDecToAltAzImpl(double ra, double dec, double latitude, double longitude, Instant time) {
        // Using the formula from http://www.stargazing.net/kepler/altaz.html
        
        double lst = getLSTImpl(time, longitude, false);
        double raDeg = ra * 15; // Convert RA from hours to degrees
        double ha = (lst - raDeg) % 360; // Clamp to 0-360

        double haRad = degToRad(ha);
        double decRad = degToRad(dec);
        double latRad = degToRad(latitude);

        double altRad = Math.asin(Math.sin(decRad) * Math.sin(latRad) + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad));
        double azRad = Math.acos((Math.sin(decRad) - Math.sin(altRad) * Math.sin(latRad)) / (Math.cos(altRad) * Math.cos(latRad)));

        if (Math.sin(haRad) >= 0) {
            azRad = 2 * Math.PI - azRad;
        }

        return new double[] {radToDeg(altRad), radToDeg(azRad)};
    }

    @Override
    @SuppressWarnings("java:S2589") // False positive
    public Mono<Instant[]> getRiseSetTimes(double ra, double dec, double latitude, double longitude, Instant baseTime, Duration howFar, double targetAltitude) {

        int maxIterations = 2048;

        double farLimitSecs = howFar.toSeconds();
        long stepSize = (long) Math.floor(farLimitSecs / maxIterations);

        
        double lastAlt = raDecToAltAzImpl(ra, dec, latitude, longitude, baseTime)[0];
        Instant riseTime = (lastAlt >= targetAltitude) ? baseTime : null;
        Instant setTime = null;

        // This is a very naive implementation, it should be improved by a proper astrophysicist.
        for (int i = 1; i < maxIterations; i++) {

            // Get the time for the current iteration
            Instant time = baseTime.plusSeconds(stepSize * i);
            // Calculate the altitude for the current iteration
            double alt = raDecToAltAzImpl(ra, dec, latitude, longitude, time)[0];

            // Check if the altitude is above or below the target altitude, and if that has changed from the last iteration
            if (alt >= targetAltitude && lastAlt < targetAltitude && riseTime == null) {
                riseTime = time;
            } else if (alt < targetAltitude && lastAlt >= targetAltitude && setTime == null) {
                setTime = time;
            }

            // Update the last altitude
            lastAlt = alt;

            // If we have both a rise and set time, we can stop
            if (riseTime != null && setTime != null) {
                break;
            }
        }

        if (riseTime == null)
            return Mono.error(new SolverException("No times found for the given parameters"));

        return Mono.just(new Instant[] {riseTime, setTime});
    }

    /**
     * Gets the Horizons System ID of a body from its name.
     * @param body
     * @return
     */
    private Mono<String> getBodyId(String body) {
        Map<String, String> params = new HashMap<>(baseParams);
        params.replace("MAKE_EPHEM", "NO");
        params.replace("OBJ_DATA", "YES");
        params.put("COMMAND", body);


        return getReq(params)
                .flatMap(response -> {
                    String[] lines = response.split("\n");
                    // Split lines[0] trimmed by multiple spaces
                    String[] firstLine = lines[1].trim().split("\\s{2,}");

                    if (firstLine.length < 1) {
                        throw new SolverException("Horizons returned an unexpected response for " + body);
                    }

                    if (firstLine[0].contains("Multiple major-bodies match")) { // If it contains this string, it's doing a major body search
                        return parseMajorBody(lines, body);
                    }

                    if (firstLine[1].contains("Small-body Index Search")) { // If the first line contains this string, it's doing a small body search
                        return parseSmallBody(lines, body);
                    }

                    if (firstLine[0].contains("Revised:") || firstLine[0].toUpperCase().contains(body.toUpperCase())) { // If it contains this string, it has found it and is a major body
                        return Mono.just(firstLine[firstLine.length - 1]);
                    }

                    if (firstLine[0].contains("JPL/HORIZONS")) { // If it contains this string, it has found it and is a small body
                        String[] recData = lines[1].trim().split("\\s{2,}");
                        return Mono.just(recData[2] + ";");
                    }
                    

                    // If it gets here, it's an unexpected response
                    return Mono.error(new SolverException("Horizons returned an unexpected response for " + body));
                });
    }

    /**
     * Parses the response from a small body search.
     * @param lines The lines of the response
     * @param body The name of the body
     * @return The Horizons System ID of the body, with added ; to indicate it's a small body
     */
    private Mono<String> parseSmallBody(String[] lines, String body) {
        int i = 3;

        // Skip until it finds the results section
        while (i < lines.length && !lines[i].contains("Matching small-bodies:")) {
            i++;
        }
        if ((i >= lines.length) || lines[i+1].contains("No matches found") || (i+4 >= lines.length)) {
            return Mono.error(new BodyNotFoundException(body));
        }
        // Return the record number of the first result
        int recNum = Integer.parseInt(lines[i+4].trim().split("\\s{2,}")[0]);
        return Mono.just("" + recNum + ";");
    }

    /**
     * Parses the response from a major body search.
     * @param lines The lines of the response
     * @param body The name of the body
     * @return The Horizons System ID of the body
     */
    private Mono<String> parseMajorBody(String[] lines, String body) {
        String regex = "Number of matches =\\s+(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(lines[lines.length - 2]);

        if (!matcher.find()) {
            return Mono.error(new SolverException("Horizons returned an unexpected response for " + body + " (can't find number of matches)"));
        }

        int numMatches = Integer.parseInt(matcher.group(1));

        int resultStart = 5;
        List<MajorSearchResult> results = new ArrayList<>(numMatches);

        // parse the results
        for (int i = 0; i < numMatches; i++) {
            String line = lines[resultStart + i];
            long id = Long.parseLong(line.substring(0, 11).trim());
            String name = line.substring(11, 46).trim();
            String designation = line.substring(46, 59).trim();
            String aliases = line.substring(59).trim();
            results.add(new MajorSearchResult(id, name, designation, aliases));
        }

        Optional<MajorSearchResult> firstResult = results
            .stream()
            .filter(res -> res.name.equalsIgnoreCase(body) || res.aliases.toUpperCase().contains(body.toUpperCase()))
            .findFirst();

        if (!firstResult.isEmpty()) {
            return Mono.just("" + firstResult.get().id);
        } else {    // Fall back to the first result
            return Mono.just("" + results.get(0).id);
        }
    }

    /**
     * Sets up a request to Horizons API.
     * @param params The parameters to send to Horizons
     * @return A Mono containing the response from Horizons
     */
    protected Mono<String> getReq(Map<String, String> params) {
        return client
            .get()
            .uri(uriBuilder -> {
                UriBuilder builder = uriBuilder.path("/horizons.api")
                    .queryParam("format", "json");

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    
                    if (!value.startsWith("'")) {
                        value = "'" + value;
                    }
                    if (!value.endsWith("'")) {
                        value = value + "'";
                    }

                    builder.queryParam(key, value);
                }

                return builder.build();
            })
            .retrieve()
            .bodyToMono(JsonNode.class)
            .doOnCancel(() -> {})
            .flatMap(json -> {
                if (json == null || !json.has("result"))
                    return Mono.error(new SolverException("Horizons returned an empty response"));
                
                if (json.has("error"))
                    return Mono.error(new SolverException(json.findValue("error").asText()));
                
                return Mono.just(json.findValue("result").asText());
            });
    }

    private static Instant parseHorizonsDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MMM-dd HH:mm")
            .withZone(ZoneOffset.UTC);
        
        return Instant.from(formatter.parse(date));
    }

    private static final String MARKER_RISE = "r";
    private static final String MARKER_SET = "s";

    private record MajorSearchResult(long id, String name, String designation, String aliases) {}

    
}
