package com.aajpm.altair.service.observatory;

import com.aajpm.altair.service.observatory.WeatherWatchService.*;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class ASCOMWeatherWatchService extends WeatherWatchService {
    
    AlpacaClient client;

    final int deviceNumber;

    private WeatherWatchCapabilities capabilities;


    ////////////////////////////// CONSTRUCTORS ///////////////////////////////
    //#region Constructors

    public ASCOMWeatherWatchService(AlpacaClient client, int deviceNumber) {
        this.client = client;
        this.deviceNumber = deviceNumber;
        this.getCapabilities().subscribe(); // attempt to get the device's capabilities.
    }

    public ASCOMWeatherWatchService(AlpacaClient client) {
        this(client, 0);
    }

    //#endregion
    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<String> getCloudCover() throws DeviceException {
        return this.getCloudCoverValue().map(clouds -> {
            if (clouds < 0.2) {
                return "Clear";
            } else if (clouds < 0.7) {
                return "Cloudy";
            } 
            return "Overcast";
        });
    }

    @Override
    public Mono<Double> getCloudCoverValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports cloud cover.    
            if (capabilities.canCloud() == CAPABILITIES_SPECIFIC)
                return this.get("cloudcover").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring cloud cover.");        
        } else { // Else, get the capabilities and check if the device supports cloud cover.
            return getCapabilities().flatMap(caps -> {
                if (caps.canCloud() == CAPABILITIES_SPECIFIC) {
                    return this.get("cloudcover").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring cloud cover."));
                }
            });
        }
    }

    @Override
    public Mono<String> getHumidity() throws DeviceException {
        return this.getHumidityValue().map(humidity -> {
            if (humidity < 0.3) {
                return "Dry";
            } else if (humidity < 0.7) {
                return "Normal";
            } 
            return "Humid";
        });
    }

    @Override
    public Mono<Double> getHumidityValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports humidity.    
            if (capabilities.canHumidity() == CAPABILITIES_SPECIFIC)
                return this.get("humidity").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring humidity.");        
        } else { // Else, get the capabilities and check if the device supports humidity.
            return getCapabilities().flatMap(caps -> {
                if (caps.canHumidity() == CAPABILITIES_SPECIFIC) {
                    return this.get("humidity").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring humidity."));
                }
            });
        }
    }

    @Override
    public Mono<String> getPressure() throws DeviceException {
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
    public Mono<Double> getPressureValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports pressure.    
            if (capabilities.canPressure() == CAPABILITIES_SPECIFIC)
                return this.get("pressure").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring pressure.");        
        } else { // Else, get the capabilities and check if the device supports pressure.
            return getCapabilities().flatMap(caps -> {
                if (caps.canPressure() == CAPABILITIES_SPECIFIC) {
                    return this.get("pressure").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring pressure."));
                }
            });
        }
    }

    @Override
    public Mono<String> getRainRate() throws DeviceException {
        return this.getRainRateValue().map(rainRate -> {
            if (rainRate < 0.01) {          // 0.01 mm per hour is the threshold for rain.
                return "Dry";
            } else if (rainRate < 2.5) {    // 2.5 mm per hour is the threshold for wet/very light rain.
                return "Wet";
            }
            return "Rain";                  // Anything above 2.5 mm per hour is considered rain.
        });
    }

    @Override
    public Mono<Double> getRainRateValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports rain.    
            if (capabilities.canRain() == CAPABILITIES_SPECIFIC)
                return this.get("rainrate").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring rain.");        
        } else { // Else, get the capabilities and check if the device supports rain.
            return getCapabilities().flatMap(caps -> {
                if (caps.canRain() == CAPABILITIES_SPECIFIC) {
                    return this.get("rainrate").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring rain."));
                }
            });
        }
    }

    @Override
    public Mono<String> getSkyBrightness() throws DeviceException {
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
    public Mono<Double> getSkyBrightnessValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports sky brightness.    
            if (capabilities.canSkyBrightness() == CAPABILITIES_SPECIFIC)
                return this.get("skybrightness").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring sky brightness.");        
        } else { // Else, get the capabilities and check if the device supports sky brightness.
            return getCapabilities().flatMap(caps -> {
                if (caps.canSkyBrightness() == CAPABILITIES_SPECIFIC) {
                    return this.get("skybrightness").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring sky brightness."));
                }
            });
        }
    }

    @Override
    public Mono<String> getSkyQuality() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports sky quality.    
            if (capabilities.canSkyQuality() == CAPABILITIES_SPECIFIC)
                return this.getSkyQualityFromSkyTemp();
            if (capabilities.canSkyQuality() == CAPABILITIES_GENERAL)
                return this.getSkyQualityFromValue();
            throw new DeviceException("This device is not capable of measuring sky quality.");        
        } else { // Else, get the capabilities and check if the device supports sky quality.
            return getCapabilities().flatMap(caps -> {
                if (capabilities.canSkyQuality() == CAPABILITIES_SPECIFIC)
                    return this.getSkyQualityFromSkyTemp();
                if (capabilities.canSkyQuality() == CAPABILITIES_GENERAL)
                    return this.getSkyQualityFromValue();
                return Mono.error(new DeviceException("This device is not capable of measuring sky quality."));
            });
        }    
    }

    private Mono<String> getSkyQualityFromSkyTemp() {
        return this.getTemperatureSkyValue().map(skyTemp -> {
            if (skyTemp < -5) {
                return "Good";
            } else if (skyTemp < 0) {
                return "Normal";
            }
            return "Bad";
        });
    }

    private Mono<String> getSkyQualityFromValue() {
        return this.getSkyQualityValue().map(skyQuality -> {
            if (skyQuality > 21) {
                return "Good";
            } else if (skyQuality > 19) {
                return "Normal";
            }
            return "Bad";
        });
    }

    @Override
    public Mono<Double> getSkyQualityValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports sky quality.    
            if (capabilities.canSkyQuality() == CAPABILITIES_SPECIFIC)
                return this.get("skyquality").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring sky quality.");        
        } else { // Else, get the capabilities and check if the device supports sky quality.
            return getCapabilities().flatMap(caps -> {
                if (caps.canSkyQuality() == CAPABILITIES_SPECIFIC) {
                    return this.get("skyquality").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring sky quality."));
                }
            });
        }
    }

    @Override
    public Mono<String> getTemperatureAmbient() throws DeviceException {
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
    public Mono<Double> getTemperatureAmbientValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports temperature.    
            if (capabilities.canTemperature() == CAPABILITIES_SPECIFIC)
                return this.get("temperature").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring ambient temperature.");        
        } else { // Else, get the capabilities and check if the device supports temperature.
            return getCapabilities().flatMap(caps -> {
                if (caps.canTemperature() == CAPABILITIES_SPECIFIC) {
                    return this.get("temperature").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring ambient temperature."));
                }
            });
        }
    }

    @Override
    public Mono<String> getTemperatureSky() throws DeviceException {
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
    public Mono<Double> getTemperatureSkyValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports temperature.    
            if (capabilities.canTemperature() == CAPABILITIES_SPECIFIC)
                return this.get("skytemperature").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring temperature.");        
        } else { // Else, get the capabilities and check if the device supports temperature.
            return getCapabilities().flatMap(caps -> {
                if (caps.canTemperature() == CAPABILITIES_SPECIFIC) {
                    return this.get("skytemperature").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring temperature."));
                }
            });
        }
    }

    @Override
    public Mono<String> getWindDirection() throws DeviceException {
        return getWindDirectionValue().map(windDirection -> {
            if (windDirection.isNaN()) return "None";
            if (windDirection > 360.0) return "Unknown";
            if (windDirection > 338.5 || windDirection < 22.5) return "N";
            if (windDirection > 22.5 && windDirection < 67.5) return "NE";
            if (windDirection > 67.5 && windDirection < 112.5) return "E";
            if (windDirection > 112.5 && windDirection < 157.5) return "SE";
            if (windDirection > 157.5 && windDirection < 202.5) return "S";
            if (windDirection > 202.5 && windDirection < 247.5) return "SW";
            if (windDirection > 247.5 && windDirection < 292.5) return "W";
            if (windDirection > 292.5 && windDirection < 338.5) return "NW";
            return "Unknown";
        });
    }

    @Override
    public Mono<Double> getWindDirectionValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports measuring wind.    
            if (capabilities.canWind() == CAPABILITIES_SPECIFIC)
                return this.get("skytemperature").map(node -> {
                    Double value = node.asDouble();
                    if (value.compareTo(0.0) == 0) return Double.NaN;   // ASCOM returns 0.0 if there is no wind, change to NaN.
                    else return value;
                });
            else
                throw new DeviceException("This device is not capable of measuring wind.");        
        } else { // Else, get the capabilities and check if the device supports measuring wind.
            return getCapabilities().flatMap(caps -> {
                if (caps.canWind() == CAPABILITIES_SPECIFIC) {
                    return this.get("skytemperature").map(node -> {
                        Double value = node.asDouble();
                        if (value.compareTo(0.0) == 0) return Double.NaN;
                        else return value;
                    });
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring wind."));
                }
            });
        }
    }

    @Override
    public Mono<Double> getWindGustSpeed() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports measuring wind.    
            if (capabilities.canWind() == CAPABILITIES_SPECIFIC)
                return this.get("windgust").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring wind.");        
        } else { // Else, get the capabilities and check if the device supports measuring wind.
            return getCapabilities().flatMap(caps -> {
                if (caps.canWind() == CAPABILITIES_SPECIFIC) {
                    return this.get("windgust").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring wind."));
                }
            });
        }
    }

    @Override
    public Mono<String> getWindSpeed() throws DeviceException {
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
    public Mono<Double> getWindSpeedValue() throws DeviceException {
        if (capabilities != null) { // If we have the capabilities, check if the device supports measuring wind.    
            if (capabilities.canWind() == CAPABILITIES_SPECIFIC)
                return this.get("windspeed").map(JsonNode::asDouble);
            else
                throw new DeviceException("This device is not capable of measuring wind.");        
        } else { // Else, get the capabilities and check if the device supports measuring wind.
            return getCapabilities().flatMap(caps -> {
                if (caps.canWind() == CAPABILITIES_SPECIFIC) {
                    return this.get("windspeed").map(JsonNode::asDouble);
                } else {
                    return Mono.error(new DeviceException("This device is not capable of measuring wind."));
                }
            });
        }
    }

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isSafe() throws DeviceException {
        // TODO Do this
        return null;
    }

    

    


    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions



    // TODO: Implement this.


    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    @Override
    public Mono<WeatherWatchCapabilities> getCapabilities() {
        if (capabilities != null)
            return Mono.just(capabilities);

        // Load capabilities from the service.
        // Check every property to see if it returns a value (return true) or throws an exception (return false).
        Mono<Boolean> canCloud = this.get("cloudcover").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canHumidity = this.get("humidity").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canPressure = this.get("pressure").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canTemperature = this.get("temperature").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canRain = this.get("rainrate").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canWind = this.get("windspeed").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        Mono<Boolean> canSkyBrightness = this.get("skybrightness").flatMap(result -> Mono.just(true)).onErrorReturn(false);
        
        // Workaround for drivers who can check sky temperature but not quality.
        Mono<Integer> canSkyQuality = this.get("skyquality")
            .flatMap(result -> Mono.just(CAPABILITIES_SPECIFIC))
            .switchIfEmpty(this.get("skytemperature").flatMap(result -> Mono.just(CAPABILITIES_GENERAL)))
            .defaultIfEmpty(CAPABILITIES_NONE);


        // Combine all the results into a single object. Given ASCOM returns numeric values, it is assumed that if a value is returned, the device supports it.
        Mono<WeatherWatchCapabilities> ret = Mono
                .zip(canCloud, canHumidity, canPressure, canTemperature, canRain, canWind, canSkyBrightness, canSkyQuality)
                .map(tuple -> new WeatherWatchCapabilities(
                    Boolean.TRUE.equals(tuple.getT1()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    Boolean.TRUE.equals(tuple.getT2()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    Boolean.TRUE.equals(tuple.getT3()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    Boolean.TRUE.equals(tuple.getT4()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    Boolean.TRUE.equals(tuple.getT5()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    Boolean.TRUE.equals(tuple.getT6()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    Boolean.TRUE.equals(tuple.getT7()) ? CAPABILITIES_SPECIFIC : CAPABILITIES_NONE,
                    tuple.getT8()                  
                ));
        
        return this.isConnected()
                .flatMap(connected ->
                    Boolean.TRUE.equals(connected) ? ret : Mono.error(new DeviceException("Device is not connected.")))
                .doOnSuccess(caps -> capabilities = caps);
    }

    //TODO
    @Override
    public Mono<WeatherWatchStatus> getStatus() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    //#endregion
    ///////////////////////////////// HELPERS /////////////////////////////////
    //#region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("observingconditions", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("observingconditions", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.put("observingconditions", deviceNumber, action, params).subscribe();
    }

    //#endregion
}
