package com.aajpm.altair.utility.solver;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import com.aajpm.altair.config.AstrometricsConfig;
import com.aajpm.altair.utility.Interval;
import com.aajpm.altair.utility.exception.BodyNotFoundException;
import com.aajpm.altair.utility.exception.SolverException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

/**
 * An {@link EphemeridesSolver} that uses the JPL Horizons API to calculate
 * ephemerides for solar system bodies. Uses a {@link WebClient} to make
 * requests to the public Horizons System API.
 * 
 * @see <a href="https://ssd.jpl.nasa.gov/?horizons">JPL Horizons</a>
 * @see <a href="https://ssd.jpl.nasa.gov/?horizons_doc">Horizons Documentation</a>
 */
public class HorizonsEphemeridesSolver extends EphemeridesSolver {

    private WebClient client;
    private Map<String, String> baseParams;

    private final Logger logger = LoggerFactory.getLogger(HorizonsEphemeridesSolver.class);

    /**
     * Creates a new {@link HorizonsEphemeridesSolver} with the given
     * {@link AstrometricsConfig}.
     * 
     * @param config The {@link AstrometricsConfig} to use, containing the
     *               observatory location and other settings.
     */
    public HorizonsEphemeridesSolver(AstrometricsConfig config) {
        super(config);
        client = WebClient.builder()
                .baseUrl("https://ssd.jpl.nasa.gov/api/")
                // Increase the maximum in-memory size to 16MB to allow for large responses
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16*1024*1024))
                .build();

        // Set the base parameters for the calls
        baseParams = new HashMap<>(10);
        baseParams.put("MAKE_EPHEM", "YES");        // Calc ephemerides
        baseParams.put("OBJ_DATA", "NO");           // Don't include object data
        baseParams.put("EPHEM_TYPE", "OBSERVER");   // Select observer ephemerides
        baseParams.put("CENTER", "coord@399");      // Center on the observatory
        baseParams.put("SITE_COORD", String.format(Locale.US,"%.4f,%.4f,%.4f", config.getSiteLongitude(), config.getSiteLatitude(), config.getSiteElevation() / 1000)); // Set the observatory coordinates
        baseParams.put("APPARENT", "REFRACTED");    // Account for atmospheric refraction
        baseParams.put("EXTRA_PREC", "YES");        // Use extra precision for coordinates
        baseParams.put("CSV_FORMAT", "YES");        // Add commas to separate values
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This method uses the Horizons API to get the ID of the body with the
     * given name. If the body is not found, a {@link BodyNotFoundException} is
     * thrown as a {@link Mono#error(Throwable)}.
     */
    @Override
    public Mono<double[]> getAltAz(String body, Instant time) {
        Map<String, String> params = new HashMap<>(baseParams);
        
        // Fix the time string to be in the correct format for Horizons
        String timeString = parseToHorizonsDate(time);
        params.put("TLIST", timeString);
        params.put("QUANTITIES", "4"); // Alt/Az
        
        return getBodyId(body).flatMap(id -> {
            params.put("COMMAND", "" + id);
            return getReq(params).flatMap(response -> {
                logger.trace("getAltAz(body = {},time = {}) Horizons response: \n{}", body, time, response);
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

    /**
     * {@inheritDoc}
     * 
     * <p>This method uses the Horizons API to get the ID of the body with the
     * given name. If the body is not found, a {@link BodyNotFoundException} is
     * thrown as a {@link Mono#error(Throwable)}.
     */
    @Override
    @SuppressWarnings("java:S3776")
    public Mono<Boolean> isVisible(String body, Instant time, double altitude) {
        
        Map<String, String> params = new HashMap<>(baseParams);
        
        // Fix the time string to be in the correct format for Horizons
        String timeString = parseToHorizonsDate(time);
        params.put("TLIST", timeString);
        params.put("QUANTITIES", "4,12"); // Alt/Az + visibility
        
        if (altitude > 0) {
            params.put("ELEV_CUT", "" + altitude);
        }
        
        return getBodyId(body).flatMap(id -> {
            params.put("COMMAND", "" + id);
            return getReq(params).flatMap(response -> {
                logger.trace("isVisible(body = {},time = {}, altitude = {}) Horizons response: \n{}", body, time, altitude, response);
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

    /**
     * {@inheritDoc}
     * 
     * <p>This method can be slow as it has to make a call to the Horizons API
     * which may contain a lot of data.
     */
    @Override
    public Mono<Interval> getNightTime(Instant start)    {
        // Check if the site is above the arctic/antartic circle
        boolean mayMidnightSun = Math.abs(this.getConfig().getSiteLatitude()) > 65;
        int searchIntervalDays = mayMidnightSun ? 200 : 1;

        Interval searchInterval = new Interval(start, Duration.ofDays(searchIntervalDays));

        Map<String, String> params = new HashMap<>(baseParams);
        baseParams.put("EXTRA_PREC", "NO");
        params.put("COMMAND", "10");
        params.put("QUANTITIES", "4");
        params.put("ANG_FORMAT", "DEG");
        params.put("SKIP_DAYLT", "NO");
        params.put("START_TIME", parseToHorizonsDate(searchInterval.getStart()));
        params.put("STOP_TIME", parseToHorizonsDate(searchInterval.getEnd()));
        params.put("STEP_SIZE", mayMidnightSun ? "15m" : "5m");
        params.put("ELEV_CUT", Double.toString(config.getDawnLine()));

        // Get the date of last line before the cutoff
        return getReq(params).flatMap(response -> {
            logger.trace("getNightTime() Horizons response: \n{}", response);
            List<String> ephemerides = Arrays.stream(response.split("\n"))
                                        .dropWhile(line -> !line.contains("$$SOE"))
                                        .skip(1)
                                        .takeWhile(line -> !line.contains("$$EOE"))
                                        .filter(line -> !line.isBlank())
                                        .map(String::trim)
                                        .collect(Collectors.toList());

            
            if (ephemerides.isEmpty() || ephemerides.get(0).contains("No ephemeris")) {
                return Mono.error(new SolverException("Horizons returned no ephemeris for Sol: " + response));
            }

            boolean isNight = ephemerides.get(0).contains("Elevation Cut-off");

            Instant retStart = isNight ? searchInterval.getStart() : null;
            Instant retEnd = null;

            if (isNight) {
                retStart = searchInterval.getStart();
                retEnd = parseHorizonsDate(ephemerides.get(1).split(",")[0].trim());

                return Mono.just(new Interval(retStart, retEnd));
            }

            // find the first "Elevation Cut-off"
            int elevCut = IntStream.range(0, ephemerides.size())
                                    .filter(i -> ephemerides.get(i).contains("Elevation Cut-off"))
                                    .findFirst()
                                    .orElse(-1);
                
            if (elevCut == -1 || elevCut >= ephemerides.size() - 2) {
                return Mono.error(new SolverException("Horizons could not find astronomical dawn"));
            }

            retStart = parseHorizonsDate(ephemerides.get(elevCut - 1).split(",")[0].trim());
            retEnd = parseHorizonsDate(ephemerides.get(elevCut + 1).split(",")[0].trim());
            return Mono.just(new Interval(retStart, retEnd));
        });   
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This method uses the Horizons API to get the ID of the body with the
     * given name. If the body is not found, a {@link BodyNotFoundException} is
     * thrown as a {@link Mono#error(Throwable)}.
     */
    @Override
    @SuppressWarnings("java:S3776")
    public Mono<Interval> getRiseSetTime(String body, Interval searchInterval, double altitude) {
        
        Map<String, String> params = new HashMap<>(baseParams);
        Instant baseTime = searchInterval.getStart();
        Instant endTime = searchInterval.getEnd();

        params.put("START_TIME", parseToHorizonsDate(baseTime));
        params.put("STOP_TIME", parseToHorizonsDate(endTime));
        params.put("STEP_SIZE", "1m TVH");  // Precision of 1 minute, highest precision available

        if (altitude != 0) {
            params.put("ELEV_CUT", "" + altitude);
        }

        params.put("QUANTITIES", "4,12"); // Alt/Az + visibility

        
        return getBodyId(body).flatMap(id -> {
            params.put("COMMAND", "" + id);
            return getReq(params).flatMap(response -> {
                logger.trace("getRiseSetTime(body = {},interval = {}, altitude = {}) Horizons response: \n{}", body, searchInterval, altitude, response);
                
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
                    // No data returned, so no rise or set times
                    return Mono.just(Interval.empty());
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

                if (riseTime == null) { // Never rose
                    return Mono.just(Interval.empty());
                }

                if (setTime == null) {  // Never set
                    setTime = endTime;
                }

                return Mono.just(new Interval(riseTime, setTime));
            });
        });
    }

    /**
     * {@inheritDoc}
     * 
     * <p>This method uses a formula from
     * {@link http://www.stargazing.net/kepler/altaz.html#twig02}. While
     * an approximation, it is accurate to within 0.3 seconds for dates
     * within 100 years of J2000.
     * 
     */
    @Override
    public Mono<Double> getLST(Instant time, double longitude, boolean useHoursInstead) {
        return Mono.just(getLSTImpl(time, longitude, useHoursInstead));
    }

    /**
     * Implementation of {@link #getLST(Instant, double, boolean)}.
     */
    protected double getLSTImpl(Instant time, double longitude, boolean useHoursInstead) {
        // Using the formula from http://www.stargazing.net/kepler/altaz.html
        double daysSinceJ2000 = getJ2000Time(time).toSeconds() / 86400.0;
        ZonedDateTime utcTime = time.atZone(ZoneOffset.UTC);
        double utc = utcTime.getHour() + utcTime.getMinute() / 60.0 + utcTime.getSecond() / 3600.0; 
        double lst = (100.46 + 0.985647 * daysSinceJ2000 + longitude + 15 * utc) % 360;
        
        return useHoursInstead ? lst / 15.0 : lst;
    }
    

    
    /**
     * {@inheritDoc}
     * 
     * <p>This method uses spherical trigonometry to calculate the altitude
     * and azimuth of a body, based on the formula from
     * {@link http://www.stargazing.net/kepler/altaz.html#twig04}. It is a
     * rough approximation, with an experimental error of approx. +/- 2%
     * 
     */
    @Override
    public Mono<double[]> raDecToAltAz(double ra, double dec, double latitude, double longitude, Instant time) {
        return Mono.just(raDecToAltAzImpl(ra, dec, latitude, longitude, time));
    }

    /**
     * Implementation of {@link #raDecToAltAz(double, double, double, double, Instant)}.
     */
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

    /**
     * {@inheritDoc}
     * 
     * @return The value 98.0, based on observations of its precision.
     */
    @Override
    public double getRaDecToAltAzPrecision() { return 98.0; }

    /**
     * {@inheritDoc}
     * 
     * <p>This method uses a naive implementation of a greedy algorithm to
     * find the rise and set times of a body, dividing the search interval
     * into equal length steps and checking the altitude at each step. Since
     * it uses {@link #raDecToAltAz(double, double, double, double, Instant)},
     * it is subject to some degree of error, but it should be almost negligible
     * for rise and set times, since the altitude changes rapidly around those
     * times and it is not advisable to be taking observations at those times.
     * 
     * @see #raDecToAltAz(double, double, double, double, Instant)
     * @see #getRaDecToAltAzPrecision()
     * 
     * @implNote This method should be overhauled to use a proper astrophysical
     *           algorithm, but it is not a priority at the moment.
     * 
     */
    @Override
    @SuppressWarnings("java:S2589") // False positive
    public Mono<Interval> getRiseSetTime(double ra, double dec, double latitude, double longitude, Interval searchInterval, double targetAltitude) {

        int maxIterations = 2048;

        double farLimitSecs = searchInterval.getDurationSeconds();
        long stepSize = (long) Math.floor(farLimitSecs / maxIterations);

        
        double lastAlt = raDecToAltAzImpl(ra, dec, latitude, longitude, searchInterval.getStart())[0];
        Instant riseTime = (lastAlt >= targetAltitude) ? searchInterval.getStart() : null;
        Instant setTime = null;

        // This is a very naive implementation, it should be improved by a proper astrophysicist.
        for (int i = 1; i < maxIterations; i++) {

            // Get the time for the current iteration
            Instant time = searchInterval.getStart().plusSeconds(stepSize * i);
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

        if (riseTime == null)   // If we didn't find a rise time, return an empty interval
            return Mono.just(Interval.empty());

        if (setTime == null)    // If we didn't find a set time, set it to the end of the search interval
            setTime = searchInterval.getEnd();

        return Mono.just(new Interval(riseTime, setTime));
    }

    /**
     * Gets the Horizons System ID of a body from its name.
     * 
     * @param body The name of the body
     * 
     * @return A {@link Mono} containing the Horizons System ID of the body, or
     *         a {@link Mono#error(Throwable)} if the body was not found.
     */
    private Mono<String> getBodyId(String body) {
        Map<String, String> params = new HashMap<>(baseParams);
        params.replace("MAKE_EPHEM", "NO");
        params.replace("OBJ_DATA", "YES");
        params.put("COMMAND", body);


        return getReq(params)
                .flatMap(response -> {
                    logger.trace("getBodyId({}) Horizons response: {}", body, response);
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
     * 
     * @param lines The lines of the response
     * @param body The name of the body
     * 
     * @return The Horizons System ID of the body, with an added ; to indicate
     *         it's a small body, or a {@link Mono#error(Throwable)} if the
     *         body was not found.
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
     * 
     * @param lines The lines of the response
     * @param body The name of the body
     * 
     * @return The Horizons System ID of the body, or a {@link Mono#error(Throwable)}
     *         if the body was not found.
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
     * 
     * @param params The parameters to send to Horizons.
     * 
     * @return A {@link Mono} containing the response from Horizons API.
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
                
                if (json.has("error")) {
                    logger.error("{}", params.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("&")));
                    return Mono.error(new SolverException(json.findValue("error").asText()));
                }
                    
                
                return Mono.just(json.findValue("result").asText());
            });
    }

    /**
     * Parses a date {@link String} from Horizons API into an
     * {@link Instant}.
     * 
     * @param date The date to parse
     * 
     * @return A {@link Mono} containing the parsed response.
     */
    private static Instant parseHorizonsDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MMM-dd HH:mm", Locale.US)
            .withZone(ZoneOffset.UTC);
        
        return Instant.from(formatter.parse(date));
    }

    /**
     * Parses an {@link Instant} into a date {@link String} for Horizons API.
     * 
     * @param date The {@link Instant} to parse.
     * @return The date {@link String} for Horizons API.
     */
    private static String parseToHorizonsDate(Instant date) {
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm", Locale.US)
            .withZone(ZoneOffset.UTC);
        
        return formatter.format(date);
    }

    private static final String MARKER_RISE = "r";
    private static final String MARKER_SET = "s";

    /**
     * A record representing a tuple from a major body search.
     */
    private record MajorSearchResult(long id, String name, String designation, String aliases) {}

    
}
