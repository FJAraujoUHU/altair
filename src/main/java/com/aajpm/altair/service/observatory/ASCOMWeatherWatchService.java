package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.aajpm.altair.utility.exception.DeviceException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuple8;

import java.util.Locale;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class ASCOMWeatherWatchService extends WeatherWatchService {

    AlpacaClient client;

    final int deviceNumber;

    private WeatherWatchCapabilities capabilities;

    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    // #region Constructors

    public ASCOMWeatherWatchService(AlpacaClient client, int deviceNumber) {
        this.client = client;
        this.deviceNumber = deviceNumber;
        this.getCapabilities().onErrorComplete().subscribe(); // attempt to get the device's capabilities.
    }

    public ASCOMWeatherWatchService(AlpacaClient client) {
        this(client, 0);
    }

    // #endregion
    ///////////////////////////////// GETTERS /////////////////////////////////
    // #region Getters

    @Override
    public Mono<String> getCloudCover() {
        return this.getCloudCoverValue().map(clouds -> {
            if (clouds < 20) {
                return "Clear";
            } else if (clouds < 70) {
                return "Cloudy";
            }
            return "Overcast";
        });
    }

    @Override
    public Mono<Double> getCloudCoverValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canCloud() == CAPABILITIES_SPECIFIC) {
                return this.get("cloudcover").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring cloud cover."));
            }
        });
    }

    @Override
    public Mono<String> getHumidity() {
        return this.getHumidityValue().map(humidity -> {
            if (humidity < 30) {
                return "Dry";
            } else if (humidity < 70) {
                return "Normal";
            }
            return "Humid";
        });
    }

    @Override
    public Mono<Double> getHumidityValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canHumidity() == CAPABILITIES_SPECIFIC) {
                return this.get("humidity").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring humidity."));
            }
        });
    }

    @Override
    public Mono<String> getPressure() {
        return this.getPressureValue().map(pressure -> {
            if (pressure < 980) {
                return "Low";
            } else if (pressure < 1030) {
                return "Normal";
            }
            return "High";
        });
    }

    @Override
    public Mono<Double> getPressureValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canPressure() == CAPABILITIES_SPECIFIC) {
                return this.get("pressure").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring pressure."));
            }
        });
    }

    @Override
    public Mono<String> getRainRate() {
        return this.getRainRateValue().map(rainRate -> {
            if (rainRate < 0.01) { // 0.01 mm per hour is the threshold for rain.
                return "Dry";
            } else if (rainRate < 2.5) { // 2.5 mm per hour is the threshold for wet/very light rain.
                return "Wet";
            }
            return "Rain"; // Anything above 2.5 mm per hour is considered rain.
        });
    }

    @Override
    public Mono<Double> getRainRateValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canRain() == CAPABILITIES_SPECIFIC) {
                return this.get("rainrate").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring rain."));
            }
        });
    }

    @Override
    public Mono<String> getSkyBrightness() {
        return this.getSkyBrightnessValue().map(brightness -> {
            if (brightness < 1.5) {
                return "Dark";
            } else if (brightness < 20) {
                return "Grey";
            }
            return "Bright";
        });
    }

    @Override
    public Mono<Double> getSkyBrightnessValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canSkyBrightness() == CAPABILITIES_SPECIFIC) {
                return this.get("skybrightness").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring sky brightness."));
            }
        });
    }

    @Override
    public Mono<String> getSkyQuality() {
        return getCapabilities().flatMap(caps -> {
            if (capabilities.canSkyQuality() == CAPABILITIES_SPECIFIC)
                return this.getSkyQualityFromSkyTemp();
            if (capabilities.canSkyQuality() == CAPABILITIES_GENERAL)
                return this.getSkyQualityFromValue();
            return Mono.error(new DeviceException("This device is not capable of measuring sky quality."));
        });
    }

    private Mono<String> getSkyQualityFromSkyTemp() {
        return this.getTemperatureSkyValue().map(skyTemp -> {
            if (skyTemp < -5) {
                return "Good";
            } else if (skyTemp < 0) {
                return "Normal";
            }
            return "Poor";
        });
    }

    private Mono<String> getSkyQualityFromValue() {
        return this.getSkyQualityValue().map(skyQuality -> {
            if (skyQuality > 21) {
                return "Good";
            } else if (skyQuality > 19) {
                return "Normal";
            }
            return "Poor";
        });
    }

    @Override
    public Mono<Double> getSkyQualityValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canSkyQuality() == CAPABILITIES_SPECIFIC) {
                return this.get("skyquality").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring sky quality."));
            }
        });
    }

    @Override
    public Mono<String> getTemperatureAmbient() {
        return getTemperatureAmbientValue().map(temperature -> {
            if (temperature < 10) {
                return "Cold";
            } else if (temperature < 25) {
                return "Normal";
            }
            return "Hot";
        });
    }

    @Override
    public Mono<Double> getTemperatureAmbientValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canTemperature() == CAPABILITIES_SPECIFIC) {
                return this.get("temperature").map(JsonNode::asDouble);
            } else {
                return Mono
                        .error(new DeviceException("This device is not capable of measuring ambient temperature."));
            }
        });
    }

    @Override
    public Mono<String> getTemperatureSky() {
        return getTemperatureSkyValue().map(temperature -> {
            if (temperature < -5) {
                return "Cold";
            } else if (temperature < 0) {
                return "Normal";
            }
            return "Hot";
        });
    }

    @Override
    public Mono<Double> getTemperatureSkyValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canTemperature() == CAPABILITIES_SPECIFIC) {
                return this.get("skytemperature").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring temperature."));
            }
        });
    }

    @Override
    public Mono<String> getWindDirection() {
        return getWindDirectionValue().map(windDirection -> {
            if (windDirection.isNaN()) {
                return "None";
            }
    
            // Normalize to 0-360 degrees
            double normalizedDirection = windDirection % 360.0;
            // Divide the circle into 8 sectors of 45 degrees each, one for each compass direction.
            // The mode 8 is to handle windDirection > 338.5 being N and not overflowing
            int sector = (int) ((normalizedDirection + 22.5) / 45.0) % 8;
    
            switch (sector) {
                case 0:
                    return "N";
                case 1:
                    return "NE";
                case 2:
                    return "E";
                case 3:
                    return "SE";
                case 4:
                    return "S";
                case 5:
                    return "SW";
                case 6:
                    return "W";
                case 7:
                    return "NW";
                default:
                    return "Unknown";
            }
        });
    }

    @Override
    public Mono<Double> getWindDirectionValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canWind() == CAPABILITIES_SPECIFIC) {
                return this.get("winddirection").map(node -> {
                    // ASCOM returns 0.0 if the wind direction is unknown/none, but we want to return NaN
                    Double value = node.asDouble();
                    if (value.compareTo(0.0) == 0)
                        return Double.NaN;
                    else
                        return value;
                });
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring wind."));
            }
        });
    }

    @Override
    public Mono<Double> getWindGustSpeed() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canWind() == CAPABILITIES_SPECIFIC) {
                return this.get("windgust").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring wind."));
            }
        });
    }

    @Override
    public Mono<String> getWindSpeed() {
        return getWindSpeedValue().map(speed -> {
            if (speed < 1.5) {
                return "Calm";
            } else if (speed < 3) {
                return "Windy";
            }
            return "Very windy";
        });
    }

    @Override
    public Mono<Double> getWindSpeedValue() {
        return getCapabilities().flatMap(caps -> {
            if (caps.canWind() == CAPABILITIES_SPECIFIC) {
                return this.get("windspeed").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("This device is not capable of measuring wind."));
            }
        });
    }

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isSafe() throws DeviceException {
        // Checks cloudcover, rainrate, skybrightness, skyquality, windspeed and
        // windgust.
        Mono<Boolean> connected = isConnected().onErrorReturn(false);
        Mono<Boolean> cloudSafe = getCloudCover().map(safe -> !"OVERCAST".equalsIgnoreCase(safe)).onErrorReturn(true);
        Mono<Boolean> rainSafe = getRainRate().map("DRY"::equalsIgnoreCase).onErrorReturn(true);
        Mono<Boolean> brightnessSafe = getSkyBrightness().map(safe -> !"BRIGHT".equalsIgnoreCase(safe)).onErrorReturn(true);
        Mono<Boolean> qualitySafe = getSkyQuality().map(safe -> !"POOR".equalsIgnoreCase(safe)).onErrorReturn(true);
        Mono<Boolean> windSafe = getWindSpeed().map(safe -> !"VERY WINDY".equalsIgnoreCase(safe)).onErrorReturn(true);
        Mono<Boolean> gustSafe = getWindGustSpeed().map(safe -> safe < 10.0).onErrorReturn(true);

        // If it's connected and all the conditions are safe, return true.
        return connected.flatMap(conn -> {
            if (Boolean.TRUE.equals(conn)) {
                // Return true if all conditions are safe.
                return Mono.zip(cloudSafe, rainSafe, brightnessSafe, qualitySafe, windSafe, gustSafe)
                    .map(tuple -> tuple.getT1() && tuple.getT2() && tuple.getT3() && tuple.getT4() && tuple.getT5()
                            && tuple.getT6());

            } else return Mono.just(false);
        });
    }

    // #endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    // #region Setters/Actions

    @Override
    public Mono<Boolean> connect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(true));
        return this.put("connected", params).thenReturn(true);
    }

    @Override
    public Mono<Boolean> disconnect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(false));
        return this.put("connected", params).thenReturn(true);
    }

    // #endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    // #region Status Reporting

    @Override
    @SuppressWarnings("java:S3776") // It really isn't that complex
    public Mono<WeatherWatchCapabilities> getCapabilities() {
        if (capabilities != null)
            return Mono.just(capabilities);

        // Load capabilities from the service.
        // Check every property to see if it returns a value (return true) or throws an
        // exception (return false).
        Mono<Boolean> canCloud = this.get("cloudcover").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canHumidity = this.get("humidity").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canPressure = this.get("pressure").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canTemperature = this.get("temperature").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canRain = this.get("rainrate").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canWind = this.get("windspeed").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canSkyBrightness = this.get("skybrightness").flatMap(result -> Mono.just(true))
                .onErrorReturn(false);

        // Workaround for drivers who can check sky temperature but not quality.
        Mono<Integer> canSkyQuality = this.get("skyquality").flatMap(result -> Mono.just(CAPABILITIES_SPECIFIC)).onErrorComplete()
                .switchIfEmpty(this.get("skytemperature").flatMap(result -> Mono.just(CAPABILITIES_GENERAL)))
                .defaultIfEmpty(CAPABILITIES_NONE)
                .onErrorReturn(CAPABILITIES_NONE);

        // Combine all the results into a single object. Given ASCOM returns numeric
        // values, it is assumed that if a value is returned, the device supports it.
        Mono<WeatherWatchCapabilities> ret = Mono
                .zip(canCloud, canHumidity, canPressure, canTemperature, canRain, canWind, canSkyBrightness,
                        canSkyQuality)
                .map(tuple -> new WeatherWatchCapabilities(
                        Boolean.TRUE.equals(tuple.getT1()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        Boolean.TRUE.equals(tuple.getT2()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        Boolean.TRUE.equals(tuple.getT3()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        Boolean.TRUE.equals(tuple.getT4()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        Boolean.TRUE.equals(tuple.getT5()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        Boolean.TRUE.equals(tuple.getT6()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        Boolean.TRUE.equals(tuple.getT7()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                        tuple.getT8()));

        return this.isConnected()
                .flatMap(connected -> Boolean.TRUE.equals(connected) ? ret : Mono.error(new DeviceException("Device is not connected.")))
                .doOnSuccess(caps -> capabilities = caps);
    }

    @Override
    public Mono<WeatherWatchStatus> getStatus() {
        Mono<Boolean> connected = this.isConnected().onErrorReturn(false);
        Mono<Boolean> safe = this.isSafe().onErrorReturn(false);
        Mono<Double> cloudCover = this.getCloudCoverValue().onErrorReturn(Double.NaN);
        Mono<Double> humidity = this.getHumidityValue().onErrorReturn(Double.NaN);
        Mono<Double> pressure = this.getPressureValue().onErrorReturn(Double.NaN);
        Mono<Double> rainRate = this.getRainRateValue().onErrorReturn(Double.NaN);
        Mono<Double> skyBrightness = this.getSkyBrightnessValue().onErrorReturn(Double.NaN);
        Mono<String> skyQuality = this.getCapabilities().flatMap(caps -> {
            if (caps.canSkyQuality() == CAPABILITIES_GENERAL)
                return this.getSkyQuality();
            if (caps.canSkyQuality() == CAPABILITIES_SPECIFIC)
                return this.getSkyQualityValue().map(value -> String.format(Locale.US,"%.2f", value));	
            return Mono.just(String.valueOf(Double.NaN));
        }).onErrorReturn("Unknown");
        Mono<Double> temperatureSky = this.getTemperatureSkyValue().onErrorReturn(Double.NaN);
        Mono<Double> temperatureAmbient = this.getTemperatureAmbientValue().onErrorReturn(Double.NaN);
        Mono<Double> windSpeed = this.getWindSpeedValue().onErrorReturn(Double.NaN);
        Mono<Double> windGust = this.getWindGustSpeed().onErrorReturn(Double.NaN);
        Mono<Double> windDirection = this.getWindDirectionValue().onErrorReturn(Double.NaN);

        // Must zip in two steps because Mono.zip() only supports up to 8 parameters.
        Mono<Tuple8<Boolean, Boolean, Double, Double, Double, Double, Double, String>> tuple1 = Mono
            .zip(
                connected,
                safe,
                cloudCover,
                humidity,
                pressure,
                rainRate,
                skyBrightness,
                skyQuality
            );
        Mono<Tuple5<Double, Double, Double, Double, Double>> tuple2 = Mono
            .zip(
                temperatureSky,
                temperatureAmbient,
                windSpeed,
                windGust,
                windDirection
            );
        
        // Combine the two TupleX into a single object.
        return Mono.zip(tuple1, tuple2).map(tuples ->
            new WeatherWatchStatus(
                tuples.getT1().getT1(),
                tuples.getT1().getT2(),
                String.format(Locale.US,"%.2f", tuples.getT1().getT3()),
                String.format(Locale.US,"%.2f", tuples.getT1().getT4()),
                String.format(Locale.US,"%.2f", tuples.getT1().getT5()),
                String.format(Locale.US,"%.2f", tuples.getT1().getT6()),
                String.format(Locale.US,"%.2f", tuples.getT1().getT7()),
                tuples.getT1().getT8(),
                String.format(Locale.US,"%.2f", tuples.getT2().getT1()),
                String.format(Locale.US,"%.2f", tuples.getT2().getT2()),
                String.format(Locale.US,"%.2f", tuples.getT2().getT3()),
                String.format(Locale.US,"%.2f", tuples.getT2().getT4()),
                String.format(Locale.US,"%.2f", tuples.getT2().getT5())
            )
        );
    }

    // #endregion
    ///////////////////////////////// HELPERS /////////////////////////////////
    // #region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("observingconditions", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("observingconditions", deviceNumber, action, params);
    }

    // #endregion
}
