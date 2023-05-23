package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;

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
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isSafe() throws DeviceException;
    
    /**
     * Returns the time since the device last updated its data
     * @return The elapsed time since the device last updated its data, in seconds
     */
    public abstract Mono<Double> getTimeSinceLastUpdate();



    /**
     * Returns the current cloud cover as a general value
     * @return The current cloud cover, either "Clear", "Cloudy" or "Overcast"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getCloudCover() throws DeviceException;

    /**
     * Returns the current cloud cover
     * @return The current cloud cover, as a 0-100 percentage
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getCloudCoverValue() throws DeviceException;


    /**
     * Returns the current humidity as a general value
     * @return The current humidity, either "Dry", "Humid" or "Wet"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getHumidity() throws DeviceException;

    /**
     * Returns the current humidity
     * @return The current humidity, as a 0-100 percentage
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getHumidityValue() throws DeviceException;


    /**
     * Returns the current pressure as a general value
     * @return The current pressure, either "Low", "Normal" or "High"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getPressure() throws DeviceException;

    /**
     * Returns the current pressure
     * @return The current pressure, in hectopascals
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getPressureValue() throws DeviceException;


    /**
     * Returns the current rain rate as a general value
     * @return The current rain rate, either "Dry", "Wet" or "Rain"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getRainRate() throws DeviceException;

    /**
     * Returns the current rain rate
     * @return The current rain rate, in millimetres per hour
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getRainRateValue() throws DeviceException;


    /**
     * Returns the current sky brightness as a general value
     * @return The current sky brightness, either "Dark", "Normal" or "Bright"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getSkyBrightness() throws DeviceException;

    /**
     * Returns the current sky brightness
     * @return The current sky brightness, in lux
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getSkyBrightnessValue() throws DeviceException;


    /**
     * Returns the currrent sky quality as a general value
     * @return The current sky quality, either "Good", "Normal" or "Bad"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getSkyQuality() throws DeviceException;

    /**
     * Returns the current sky quality
     * @return The current sky quality, in magnitude per square arcsecond
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getSkyQualityValue() throws DeviceException;


    /**
     * Returns the current sky temperature as a general value
     * @return The current sky temperature, either "Hot", "Cold" or "Normal"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getTemperatureSky() throws DeviceException;


    /**
     * Returns the current sky temperature
     * @return The current sky temperature, in degrees Celsius
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getTemperatureSkyValue() throws DeviceException;


    /**
     * Returns the current ambient temperature as a general value
     * @return The current ambient temperature, either "Hot", "Cold" or "Normal"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getTemperatureAmbient() throws DeviceException;

    /**
     * Returns the current ambient temperature
     * @return The current ambient temperature, in degrees Celsius
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getTemperatureAmbientValue() throws DeviceException;


    /**
     * Returns the current wind speed as a general value
     * @return The current wind speed, either "Calm", "Windy" or "Very windy"
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getWindSpeed() throws DeviceException;

    /**
     * Returns the current wind speed
     * @return The current wind speed, in metres per second
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getWindSpeedValue() throws DeviceException;


    /**
     * Returns the current wind direction as a general value
     * @return The current wind direction, as "N", "NE", "E", "SE", "S", "SW", "W" or "NW", or "None" if there is no wind
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<String> getWindDirection() throws DeviceException;


    /**
     * Returns the current wind direction
     * @return The current wind direction, in degrees clockwise from North, or NaN if there is no wind
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getWindDirectionValue() throws DeviceException;


    /**
     * Returns the current wind gust speed
     * @return The current wind gust speed (Max value in the last 2 mins), in metres per second
     * @throws DeviceException If there was an error polling the data or the device does not support this feature.
     */
    public abstract Mono<Double> getWindGustSpeed() throws DeviceException;

    //#endregion
    ///////////////////////////////// SETTERS /////////////////////////////////
    //#region Setters

    /**
     * Connects the device
     * @return A Mono that completes when the device has been connected.
     * @throws DeviceException If there was an error connecting the weather watch.
     */
    public abstract Mono<Void> connect() throws DeviceException;

    /**
     * Disconnects the device
     * @return A Mono that completes when the device has been disconnected.
     * @throws DeviceException If there was an error disconnecting the weather watch.
     */
    public abstract Mono<Void> disconnect() throws DeviceException;

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Returns the capabilities of the device
     * @return A WeatherWatchStatus object containing the capabilities of the weather watch
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<WeatherWatchCapabilities> getCapabilities();

    /**
     * Returns the status of the device
     * @return A WeatherWatchStatus object containing the status of the weather watch
     * @throws DeviceException If there was an error polling the data.
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
        String skyQuality,          // Sky quality, either "Good", "Normal" or "Bad", or in magnitude/arcsec^2
        String temperatureSky,      // Sky temperature (usually mesured using IR), either "Hot", "Cold" or  "Normal", or in °C
        String temperatureAmbient,  // Ambient temperature, either "Hot", "Cold" or "Normal", or in °C
        String windSpeed,           // Wind speed, either "Calm", "Windy" or "Very windy", or in m/s
        String windGust,            // Wind gust (max peak in the last 2 mins), in m/s
        String windDirection        // Wind direction, either using cardinal directions or in degrees clockwise starting from north. If there is no wind, this is "None"
    ) {}

    //#endregion

    
}
