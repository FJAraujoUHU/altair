package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import com.aajpm.altair.config.ObservatoryConfig.CameraConfig;

import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.util.FitsOutputStream;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;

/*
 * TODO: Might add Gain control in future release
 */
public abstract class CameraService {
    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    protected CameraConfig config;

    //#endregion
    ////////////////////////////// CONSTRUCTOR /////////////////////////////////
    //#region Constructor

    /**
     * Creates a new instance of the CameraService class
     */
    protected CameraService(CameraConfig config) {
        this.config = config;
    }

    //#endregion
    //////////////////////////////// GETTERS //////////////////////////////////
    //#region Getters

    /**
     * Returns true if the camera is connected
     * @return true if the camera is connected, false otherwise
     */
    public abstract Mono<Boolean> isConnected();

    //#region Temperature info

    /**
     * Returns the current temperature of the sensor
     * @return the current temperature of the sensor in degrees Celsius
     * @throws DeviceException if there was an error polling the data or the camera does not have this feature.
     */
    public abstract Mono<Double> getTemperature() throws DeviceException;

    /**
     * Returns the current target temperature of the sensor
     * @return the current target temperature of the sensor in degrees Celsius
     * @throws DeviceException if there was an error polling the data or the camera does not have this feature.
     */
    public abstract Mono<Double> getTemperatureTarget() throws DeviceException;

    /**
     * Returns the current ambient temperature. This might also be called heat sink temperature.
     * @return the current ambient temperature in degrees Celsius
     * @throws DeviceException if there was an error polling the data or the camera does not have this feature. 
     */
    public abstract Mono<Double> getTemperatureAmbient() throws DeviceException;

    /**
     * Checks if the cooler is on
     * @return true if the cooler is on, false otherwise.
     * @throws DeviceException if there was an error polling the data or the camera does not have this feature.
     */
    public Mono<Boolean> isCoolerOn() throws DeviceException {
        return getCoolerStatus().map(status -> (status != COOLER_OFF) && (status != COOLER_ERROR));
    }

    /**
     * Returns the current power level of the cooler
     * @return the current cooler power level percentage in the range [0.0-1.0]
     * @throws DeviceException if there was an error polling the data or the camera does not have this feature.
     */
    public abstract Mono<Double> getCoolerPower() throws DeviceException;

    /**
     * Returns the current status of the cooler
     * @return the current status of the cooler as an integer: 0 = Off, 1 = Cooling down, 2 = Warming up, 3 = Stabilized, 4 = Saturated, 5 = Error
     * @throws DeviceException
     */
    public abstract Mono<Integer> getCoolerStatus() throws DeviceException;

    //#endregion


    //#region Exposure parameters

    /**
     * Returns the current subframe width
     * @return the current subframe width
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubframeWidth() throws DeviceException;

    /**
     * Returns the current subframe height
     * @return the current subframe height
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubframeHeight() throws DeviceException;

    /**
     * Returns the current subframe dimensions
     * @return the current subframe size as a tuple of integers in the form (width, height)
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple2<Integer, Integer>> getSubframeSize() throws DeviceException {
        return Mono.zip(getSubframeWidth(), getSubframeHeight());
    }

    /**
     * Returns the current subframe start position on the X axis
     * @return the current subframe start position on the X axis
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubframeStartX() throws DeviceException;

    /**
     * Returns the current subframe start position on the Y axis
     * @return the current subframe start position on the Y axis
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubframeStartY() throws DeviceException;

    /**
     * Returns the current subframe start position coordinates
     * @return the current subframe start position as a tuple of integers in the form (x, y)
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple2<Integer, Integer>> getSubframeStartPos() throws DeviceException {
        return Mono.zip(getSubframeStartX(), getSubframeStartY());
    }

    /**
     * Returns the current subframe info
     * @return the current subframe definition as a tuple of integers in the form (x, y, width, height)
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple4<Integer, Integer, Integer, Integer>> getSubFrame() throws DeviceException {
        return Mono.zip(getSubframeStartX(), getSubframeStartY(), getSubframeWidth(), getSubframeHeight());
    }

    /**
     * Returns the current binning of the camera on the X axis
     * @return the current binning of the camera as an integer
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getBinningX() throws DeviceException;

    /**
     * Returns the current binning of the camera on the Y axis
     * @return the current binning of the camera as an integer
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getBinningY() throws DeviceException;

    /**
     * Returns the current binning of the camera
     * @return the current binning of the camera as a tuple of integers
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple2<Integer, Integer>> getBinning() throws DeviceException {
        return Mono.zip(getBinningX(), getBinningY());
    }

    //#endregion

    //#region Image readout

    /**
     * Checks if the camera has an image ready to be downloaded
     * @return true if the camera has an image ready to be downloaded, false otherwise.
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Boolean> isImageReady() throws DeviceException;

    /**
     * Returns the capture image as a ImageHDU object
     * @return the capture image as a ImageHDU object
     */
    public abstract Mono<ImageHDU> getImage() throws DeviceException;

    // TODO: Probably should move this to Observatory class to add extra metadata
    /**
     * Saves the captured image to a FITS file in the image store directory
     * @param name the name of the file to save the image to
     * @param compression whether to output a GZIP'd file or just a plain FITS file
     */
    public void saveImage(String name, boolean compression) throws DeviceException {
        this.getImage().subscribe(image -> {
            String filename = name;
            try {

                if (!Files.exists(config.getImageStorePath())) {
                    Files.createDirectories(config.getImageStorePath());
                }

                // Cleans up the filename to remove any illegal characters
                filename = filename.replaceAll("[^a-zA-Z0-9\\._\\-]", "_");
                
                // Add .fit extension if not present   
                if (!filename.toUpperCase().endsWith(".FIT") && !filename.toUpperCase().endsWith(".FITS")) {
                    filename += ".fit";
                }

                Fits fits = new Fits();
                fits.addHDU(image);

                FitsOutputStream out;
                Path filepath;
                // Set up output stream and compression.
                if (compression) {
                    filename += ".gz";
                    filepath = config.getImageStorePath().resolve(filename);
                    out = new FitsOutputStream(
                            new GZIPOutputStream(
                                Files.newOutputStream(
                                    filepath
                                )
                            )
                        );
                } else {
                    filepath = config.getImageStorePath().resolve(filename);
                    out = new FitsOutputStream(
                            Files.newOutputStream(
                                filepath
                            )
                        );
                }
                // Write the image to the file and close the stream.
                fits.write(out);
                out.close();
                fits.close();
                
            } catch (Exception e) { // If there was an error, print the stack trace.
                e.printStackTrace();
            }
        });   
    }

    /**
     * Dumps the unprocessed image as the camera's native format to a file in the image store directory
     * @param name the name of the file to save the image to
     */
    public abstract void dumpImage(String name) throws DeviceException;

    //#endregion

    
    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects to the camera
     * @throws DeviceException if there was an error connecting to the camera.
     */
    public abstract Mono<Void> connect() throws DeviceException;

    /**
     * Disconnects from the camera
     * @throws DeviceException if there was an error disconnecting from the camera.
     */
    public abstract Mono<Void> disconnect() throws DeviceException;


    //#region Cooler

    /**
     * Turns the cooler on or off
     * @param enable true to turn the cooler on, false to turn it off
     * @throws DeviceException if there was an error setting the cooler.
     */
    public abstract Mono<Void> setCooler(boolean enable) throws DeviceException;

    /**
     * Sets the target temperature of the sensor
     * @param temp the target temperature of the sensor in degrees Celsius
     * @throws DeviceException if there was an error setting the cooler.
     */
    public abstract Mono<Void> setTargetTemp(double temp) throws DeviceException;

    /**
     * Warms up the sensor to ambient temperature
     * @throws DeviceException if there was an error warming up the sensor.
     */
    public abstract Mono<Void> warmup() throws DeviceException;

    /**
     * Warms up the sensor to the specified temperature
     * @param target the target temperature in degrees Celsius
     * @throws DeviceException if there was an error warming up the sensor.
     */
    public abstract Mono<Void> warmup(double target) throws DeviceException;

    /**
     * Cools down the sensor to the specified temperature
     * @param target the target temperature in degrees Celsius
     * @throws DeviceException if there was an error cooling down the sensor.
     */
    public abstract Mono<Void> cooldown(double target) throws DeviceException;

    //#endregion


    //#region Exposure

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param startX the X coordinate of the top left corner of the subframe
     * @param startY the Y coordinate of the top left corner of the subframe
     * @param width the width of the subframe
     * @param height the height of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public Mono<Void> setSubframe(int startX, int startY, int width, int height) throws DeviceException {
        return Mono.whenDelayError(
            setSubframeStartX(startX),
            setSubframeStartY(startY),
            setSubframeWidth(width),
            setSubframeHeight(height)
        );
    }

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param startX the X coordinate of the top left corner of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract Mono<Void> setSubframeStartX(int startX) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param startY the Y coordinate of the top left corner of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract Mono<Void> setSubframeStartY(int startY) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param width the width of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract Mono<Void> setSubframeWidth(int width) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param height the height of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract Mono<Void> setSubframeHeight(int height) throws DeviceException;

    /**
     * Sets the binning of the camera. If the camera does not support asymetric binning, binX will be applied to both axes.
     * @param binX the binning in the X direction
     * @param binY the binning in the Y direction
     * @throws DeviceException if there was an error setting the binning or the sensor does not support the requested binning.
     */
    public abstract Mono<Void> setBinning(int binX, int binY) throws DeviceException;

    /**
     * Sets symetrical binning of the camera.
     * @param bin the binning in both directions
     * @throws DeviceException if there was an error setting the binning or the sensor does not support the requested binning.
     */
    public abstract Mono<Void> setBinning(int bin) throws DeviceException;

    /**
     * Starts an exposure.
     * @param duration the exposure time in seconds
     * @param useLightFrame true to use a light frame, false to use a dark frame
     * @throws DeviceException if there was an error starting the exposure.
     */
    public abstract Mono<Void> startExposure(double duration, boolean useLightFrame) throws DeviceException;

    /**
     * Starts an exposure.
     * @param duration the exposure time in seconds
     * @param useLightFrame true to use a light frame, false to use a dark frame
     * @param subframe the subframe to use for the exposure, in the format [startX, startY, width, height]
     * @param binX the binning in the X direction
     * @param binY the binning in the Y direction
     * @throws DeviceException if there was an error starting the exposure.
     */
    public Mono<Void> startExposure(double duration, boolean useLightFrame, int[] subframe, int binX, int binY) throws DeviceException {
        return Mono.when(
            setSubframe(subframe[0], subframe[1], subframe[2], subframe[3]),
            setBinning(binX, binY),
            startExposure(duration, useLightFrame)
        );
    }

    /**
     * Stops the current exposure early, if any. The exposure will not be discarded.
     * @throws DeviceException if there was an error stopping the exposure.
     */
    public abstract Mono<Void> stopExposure() throws DeviceException;

    /**
     * Aborts and discards the current exposure, if any.
     * @throws DeviceException if there was an error discarding the exposure.
     */
    public abstract Mono<Void> abortExposure() throws DeviceException;

    //#endregion


    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Returns the capabilities of the device
     * @return A CameraCapabilities object containing the capabilities of the device
     */
    public abstract Mono<CameraCapabilities> getCapabilities();

    /**
     * Returns the current status of the exposing camera
     * @return the current status of the camera as an integer: 0 = Idle,1 = Waiting, 2 = Exposing, 3 = Reading, 4 = Downloading, 5 = Error
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getCameraState() throws DeviceException;

    /**
     * Returns the current status of the exposing camera
     * @return the current status of the camera as a string
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<String> getCameraStateString() throws DeviceException {
        return Mono.zip(getCameraState(), getStatusCompletion())
            .map(tuple -> {
                String statusStr;
                switch (tuple.getT1()) {
                    case STATUS_IDLE:
                        statusStr = "Idle";
                        break;
                    case STATUS_WAITING:
                        statusStr = "Waiting";
                        break;
                    case STATUS_EXPOSING:
                        statusStr = "Exposing";
                        break;
                    case STATUS_READING:
                        statusStr = "Reading";
                        break;
                    case STATUS_DOWNLOADING:
                        statusStr = "Downloading";
                        break;
                    case STATUS_ERROR:
                        statusStr = "Error";
                        break;
                    default:
                        statusStr = "Unknown";
                        break;
                }
                if ((tuple.getT2() != null) && !tuple.getT2().isNaN()) {
                    statusStr += String.format(" (%.2f%%)", tuple.getT2() * 100);
                }
                return statusStr;
            });
    }

    /**
     * Returns the progress of the current status
     * @return the progress of the current status as a percentage, if applicable.
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Double> getStatusCompletion() throws DeviceException;

    /**
     * Returns the status of the device
     * @return A CameraStatus object containing the status of the device
     */
    public abstract Mono<CameraStatus> getStatus();

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    // Camera status codes
    public static final int STATUS_IDLE = 0;        // Available to start exposure
    public static final int STATUS_WAITING = 1;     // Exposure started but waiting
    public static final int STATUS_EXPOSING = 2;    // Expoure in progress
    public static final int STATUS_READING = 3;     // Reading from CCD
    public static final int STATUS_DOWNLOADING = 4; // Downloading to PC
    public static final int STATUS_ERROR = 5;       // Camera disabled due to error

    // Cooler status codes
    public static final int COOLER_OFF = 0;         // Cooler is off
    public static final int COOLER_COOLDOWN = 1;    // Cooler is on and slowly cooling down
    public static final int COOLER_WARMUP = 2;      // Cooler is on and slowly warming up
    public static final int COOLER_ACTIVE = 3;      // Cooler is on
    public static final int COOLER_STABLE = 4;      // Cooler is on and temperature is stable
    public static final int COOLER_SATURATED = 5;   // Cooler is on but power is very high and temperature might not be stable
    public static final int COOLER_ERROR = 6;       // Cooler disabled due to error

    /**
     * A record containing the device capabilities
     */
    public record CameraCapabilities(
        boolean canAbortExposure,       // Can the exposure be aborted? That is, can the exposure be stopped and the data discarded?
        boolean canStopExposure,        // Can the exposure be stopped? That is, can the exposure be stopped and the data retained?
        boolean canBinning,             // Can the sensor use binning?
        boolean canAsymBinning,         // Can the sensor use asymmetric binning?
        boolean canSetCoolerTemp,       // Can the cooler temperature be set?
        boolean canGetCoolerPower,      // Does the cooler report its power?
        boolean canCoolerAutoRamp,      // Can the cooler ramp up and down automatically?
        String sensorName,              // The name of the sensor (e.g. "KAF-8300")
        String sensorType,              // The type of sensor (e.g. "Monochrome", "Color", "RGGB", etc.)
        int bayOffX,                    // The X offset of the Bayer matrix, if any                   
        int bayOffY,                    // The Y offset of the Bayer matrix, if any
        int sensorX,                    // The width of the sensor in real pixels, without binning
        int sensorY,                    // The height of the sensor in real pixels, without binning
        int maxBinX,                    // The maximum binning in the X axis
        int maxBinY,                    // The maximum binning in the Y axis
        double exposureMin,             // The minimum exposure time in seconds
        double exposureMax              // The maximum exposure time in seconds
    ) {}

    /**
     * A record containing the camera status
     */
    public record CameraStatus(
        boolean connected,              // Is the camera connected?
        double temperature,             // The current temperature of the sensor
        String coolerStatus,            // The current status of the cooler
        double coolerPower,             // The current power of the cooler
        String status,                  // The current status of the camera
        String binning,                 // The current binning of the camera
        double statusCompletion,        // The completion of the current status, as a percentage (0-100)
        int sfWidth,                    // The width of the subframe
        int sfHeight,                   // The height of the subframe
        int sfX,                        // The X offset of the subframe
        int sfY                         // The Y offset of the subframe
    ) {
        public CameraStatus(boolean connected, double temperature, int coolerStatus, double coolerPower, int status, int binX, int binY, double statusCompletion, int sfWidth, int sfHeight, int sfX, int sfY) {
            this(connected, temperature, getStrCoolerStatus(coolerStatus), coolerPower, getStrStatus(status), String.format("%dx%d", binX, binY), statusCompletion, sfWidth, sfHeight, sfX, sfY);
        }

        // Helper methods to convert status codes to strings
        private static String getStrCoolerStatus(int coolerStatus) {
            switch (coolerStatus) {
                case COOLER_OFF:
                    return "Off";
                case COOLER_COOLDOWN:
                    return "Cooling down";
                case COOLER_WARMUP:
                    return "Warming up";
                case COOLER_ACTIVE:
                    return "Active";
                case COOLER_STABLE:
                    return "Stable";
                case COOLER_SATURATED:
                    return "Saturated";
                case COOLER_ERROR:
                    return "Error";
                default:
                    return "Unknown";
            }
        }

        private static String getStrStatus(int status) {
            switch (status) {
                case STATUS_IDLE:
                    return "Idle";
                case STATUS_WAITING:
                    return "Waiting";
                case STATUS_EXPOSING:
                    return "Exposing";
                case STATUS_READING:
                    return "Reading";
                case STATUS_DOWNLOADING:
                    return "Downloading";
                case STATUS_ERROR:
                    return "Error";
                default:
                    return "Unknown";
            }
        }
    }


    
}
