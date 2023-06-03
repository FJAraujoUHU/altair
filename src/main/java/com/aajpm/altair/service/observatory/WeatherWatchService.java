package com.aajpm.altair.service.observatory;

import reactor.core.publisher.Mono;

/**
 * A service for interacting with a weather watch device, such as a cloud sensor or rain sensor.
 * Definition is fairly lenient, as it allows reporting data in different ways, and allows for
 * implementation of features on demand.
 * 
 * Definition heavily based on the ASCOM ObservingConditions specification.
 */
public abstract class WeatherWatchService {



    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    /**
     * Returns whether the weather watch is connected
     * @return TRUE if connected, FALSE otherwise
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * Returns whether it is safe to operate the telescope according to the device.
     * @return TRUE if safe, FALSE if it is raining or the sun is out, etc.
     */
    public abstract Mono<Boolean> isSafe();

    /**
     * Returns the current cloud cover as a general value
     * @return The current cloud cover, either "Clear", "Cloudy" or "Overcast"
     */
    public abstract Mono<String> getCloudCover();

    /**
     * Returns the current cloud cover
     * @return The current cloud cover, as a 0-100 percentage
     */
    public abstract Mono<Double> getCloudCoverValue();


    /**
     * Returns the current humidity as a general value
     * @return The current humidity, either "Dry", "Humid" or "Wet"
     */
    public abstract Mono<String> getHumidity();

    /**
     * Returns the current humidity
     * @return The current humidity, as a 0-100 percentage
     */
    public abstract Mono<Double> getHumidityValue();


    /**
     * Returns the current pressure as a general value
     * @return The current pressure, either "Low", "Normal" or "High"
     */
    public abstract Mono<String> getPressure();

    /**
     * Returns the current pressure
     * @return The current pressure, in hectopascals
     */
    public abstract Mono<Double> getPressureValue();


    /**
     * Returns the current rain rate as a general value
     * @return The current rain rate, either "Dry", "Wet" or "Rain"
     */
    public abstract Mono<String> getRainRate();

    /**
     * Returns the current rain rate
     * @return The current rain rate, in millimetres per hour
     */
    public abstract Mono<Double> getRainRateValue();


    /**
     * Returns the current sky brightness as a general value
     * @return The current sky brightness, either "Dark", "Normal" or "Bright"
     */
    public abstract Mono<String> getSkyBrightness();

    /**
     * Returns the current sky brightness
     * @return The current sky brightness, in lux
     */
    public abstract Mono<Double> getSkyBrightnessValue();


    /**
     * Returns the currrent sky quality as a general value
     * @return The current sky quality, either "Good", "Normal" or "Bad"
     */
    public abstract Mono<String> getSkyQuality();

    /**
     * Returns the current sky quality
     * @return The current sky quality, in magnitude per square arcsecond
     */
    public abstract Mono<Double> getSkyQualityValue();


    /**
     * Returns the current sky temperature as a general value
     * @return The current sky temperature, either "Hot", "Cold" or "Normal"
     */
    public abstract Mono<String> getTemperatureSky();


    /**
     * Returns the current sky temperature
     * @return The current sky temperature, in degrees Celsius
     */
    public abstract Mono<Double> getTemperatureSkyValue();


    /**
     * Returns the current ambient temperature as a general value
     * @return The current ambient temperature, either "Hot", "Cold" or "Normal"
     */
    public abstract Mono<String> getTemperatureAmbient();

    /**
     * Returns the current ambient temperature
     * @return The current ambient temperature, in degrees Celsius
     */
    public abstract Mono<Double> getTemperatureAmbientValue();


    /**
     * Returns the current wind speed as a general value
     * @return The current wind speed, either "Calm", "Windy" or "Very windy"
     */
    public abstract Mono<String> getWindSpeed();

    /**
     * Returns the current wind speed
     * @return The current wind speed, in metres per second
     */
    public abstract Mono<Double> getWindSpeedValue();


    /**
     * Returns the current wind direction as a general value
     * @return The current wind direction, as "N", "NE", "E", "SE", "S", "SW", "W" or "NW", or "None" if there is no wind
     */
    public abstract Mono<String> getWindDirection();


    /**
     * Returns the current wind direction
     * @return The current wind direction, in degrees clockwise from North, or NaN if there is no wind
     */
    public abstract Mono<Double> getWindDirectionValue();


    /**
     * Returns the current wind gust speed
     * @return The current wind gust speed (Max value in the last 2 mins), in metres per second
     */
    public abstract Mono<Double> getWindGustSpeed();

    //#endregion
    ///////////////////////////////// SETTERS /////////////////////////////////
    //#region Setters

    /**
     * Connects the device
     * @return A Mono that completes when the device has been connected.
     */
    public abstract Mono<Void> connect();

    /**
     * Disconnects the device
     * @return A Mono that completes when the device has been disconnected.
     */
    public abstract Mono<Void> disconnect();

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Returns the capabilities of the device
     * @return A WeatherWatchStatus object containing the capabilities of the weather watch
     */
    public abstract Mono<WeatherWatchCapabilities> getCapabilities();

    /**
     * Returns the status of the device
     * @return A WeatherWatchStatus object containing the status of the weather watch
     */
    public abstract Mono<WeatherWatchStatus> getStatus();

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    public static final int CAPABILITIES_NONE = 0;      // Can't check this value
    public static final int CAPABILITIES_GENERAL = 1;   // Can check, returns a general range
    public static final int CAPABILITIES_SPECIFIC = 2;  // Can check, returns a specific value in the expected unit
    
    /**
     * A record containing the device capabilities
     */
    public record WeatherWatchCapabilities(
        int canCloud,                   // Whether the device can check for cloud cover
        int canHumidity,                // Whether the device can measure humidity
        int canPressure,                // Whether the device can measure pressure
        int canTemperature,             // Whether the device can measure temperature
        int canRain,                    // Whether the device can measure rain rate
        int canWind,                    // Whether the device can measure wind speed and direction
        int canSkyBrightness,           // Whether the device can measure sky brightness
        int canSkyQuality               // Whether the device can measure sky quality
    ) {}

    /**
     * A record containing the device status
     */
    public record WeatherWatchStatus(
        boolean connected,          // Whether the device is connected
        boolean isSafe,             // Whether it is safe to opeate the telescope
        String cloudCover,          // Cloud cover, either "Clear", "Cloudy" or "Overcast", or a percentage (0-100)
        String humidity,            // Humidity, either "Dry", "Normal" or "Humid", or a percent (0-100)
        String pressure,            // Pressure, eihter "Low", "Normal" or "High", or in hPa
        String rainRate,            // Rain rate, either "Dry", "Wet" or "Rain", or in mm/h
        String skyBrightness,       // Sky brightness, either "Dark", "Bright" or "Very bright", or in lux
        String skyQuality,          // Sky quality, either "Good", "Normal" or "Poor", or in magnitude/arcsec^2
        String temperatureSky,      // Sky temperature (usually mesured using IR), either "Hot", "Cold" or  "Normal", or in °C
        String temperatureAmbient,  // Ambient temperature, either "Hot", "Cold" or "Normal", or in °C
        String windSpeed,           // Wind speed, either "Calm", "Windy" or "Very windy", or in m/s
        String windGust,            // Wind gust (max peak in the last 2 mins), in m/s
        String windDirection        // Wind direction, either using cardinal directions or in degrees clockwise starting from north. If there is no wind, this is "None"
    ) {}

    //#endregion

    
}
