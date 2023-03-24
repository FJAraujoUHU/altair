package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.statusreporting.CameraStatus;

import nom.tam.fits.Fits;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;

public abstract class CameraService {
    /////////////////////////////// CONSTANTS /////////////////////////////////
    //#region Constants

    // Camera status codes
    public static final int STATUS_IDLE = 0;        // Available to start exposure
    public static final int STATUS_WAITING = 1;     // Exposure started but waiting
    public static final int STATUS_EXPOSING = 2;    // Expoure in progress
    public static final int STATUS_READING = 3;     // Reading from CCD
    public static final int STATUS_DOWNLOADING = 4; // Downloading to PC
    public static final int STATUS_ERROR = 5;       // Camera disabled due to error

    //
    
    //#endregion
    //////////////////////////////// GETTERS //////////////////////////////////
    //#region Getters

    /**
     * Returns true if the camera is connected
     * @return true if the camera is connected, false otherwise
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * Returns the current temperature of the sensor
     * @return the current temperature of the sensor in degrees Celsius
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Double> getTemperature() throws DeviceException;

    /**
     * Checks if the cooler is on
     * @return true if the cooler is on, false otherwise.
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Boolean> isCoolerOn() throws DeviceException;

    /**
     * Returns the current power level of the cooler
     * @return the current cooler power level percentage
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Double> getCoolerPower() throws DeviceException;

    /**
     * Returns the current status of the camera
     * @return the current status of the camera as an integer: 0 = Idle,1 = Waiting, 2 = Exposing, 3 = Reading, 4 = Downloading, 5 = Error
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getStatus() throws DeviceException;

    //#region Exposure parameters

    /**
     * Returns the current subframe width
     * @return the current subframe width
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubFrameWidth() throws DeviceException;

    /**
     * Returns the current subframe height
     * @return the current subframe height
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubFrameHeight() throws DeviceException;

    /**
     * Returns the current subframe dimensions
     * @return the current subframe size as a tuple of integers in the form (width, height)
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple2<Integer, Integer>> getSubFrameSize() throws DeviceException {
        return Mono.zip(getSubFrameWidth(), getSubFrameHeight());
    }

    /**
     * Returns the current subframe start position on the X axis
     * @return the current subframe start position on the X axis
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubFrameStartX() throws DeviceException;

    /**
     * Returns the current subframe start position on the Y axis
     * @return the current subframe start position on the Y axis
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Integer> getSubFrameStartY() throws DeviceException;

    /**
     * Returns the current subframe start position coordinates
     * @return the current subframe start position as a tuple of integers in the form (x, y)
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple2<Integer, Integer>> getSubFrameStartPos() throws DeviceException {
        return Mono.zip(getSubFrameStartX(), getSubFrameStartY());
    }

    /**
     * Returns the current subframe info
     * @return the current subframe definition as a tuple of integers in the form (x, y, width, height)
     * @throws DeviceException if there was an error polling the data.
     */
    public Mono<Tuple4<Integer, Integer, Integer, Integer>> getSubFrame() throws DeviceException {
        return Mono.zip(getSubFrameStartX(), getSubFrameStartY(), getSubFrameWidth(), getSubFrameHeight());
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

    /**
     * Returns the current status of the camera
     * @return the current status of the camera as a string
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<String> getStatusString() throws DeviceException;

    /**
     * Returns the progress of the current status
     * @return the progress of the current status as a percentage. If the status does not have a progress, returns 0.
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Double> getStatusCompletion() throws DeviceException;

    /**
     * Checks if the camera has an image ready to be downloaded
     * @return true if the camera has an image ready to be downloaded, false otherwise.
     * @throws DeviceException if there was an error polling the data.
     */
    public abstract Mono<Boolean> isImageReady() throws DeviceException;

    /**
     * Returns the capture image as a FITS object
     * @return the capture image as a FITS object
     */
    public abstract Mono<Fits> getImage() throws DeviceException;

    public Mono<CameraStatus> getCameraStatus() throws DeviceException {

        Mono<Boolean> connected = isConnected();
        Mono<Double> temperature = getTemperature();
        Mono<Boolean> coolerOn = isCoolerOn();
        Mono<Double> coolerPower = getCoolerPower();
        Mono<Integer> status = getStatus();
        Mono<Tuple2<Integer, Integer>> binning = getBinning();
        Mono<Double> statusCompletion = getStatusCompletion();
        Mono<Tuple4<Integer, Integer, Integer, Integer>> subFrame = getSubFrame();

        return Mono
            .zip(connected, temperature, coolerOn, coolerPower, status, binning, statusCompletion, subFrame)
            .map(tuple -> {
                CameraStatus cameraStatus = new CameraStatus();
                cameraStatus.setConnected(tuple.getT1());
                cameraStatus.setTemperature(tuple.getT2());
                cameraStatus.setCoolerOn(tuple.getT3());
                cameraStatus.setCoolerPower(tuple.getT4());
                cameraStatus.setStatus(tuple.getT5());
                cameraStatus.setBinning(tuple.getT6().getT1(), tuple.getT6().getT2());
                cameraStatus.setStatusCompletion(tuple.getT7());
                cameraStatus.setSubframe(tuple.getT8().getT1(), tuple.getT8().getT2(), tuple.getT8().getT3(), tuple.getT8().getT4());
                return cameraStatus;
            });
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects to the camera
     * @throws DeviceException if there was an error connecting to the camera.
     */
    public abstract void connect() throws DeviceException;

    /**
     * Disconnects from the camera
     * @throws DeviceException if there was an error disconnecting from the camera.
     */
    public abstract void disconnect() throws DeviceException;

    /**
     * Turns the cooler on or off
     * @param enable true to turn the cooler on, false to turn it off
     * @throws DeviceException if there was an error setting the cooler.
     */
    public abstract void setCooler(boolean enable) throws DeviceException;

    /**
     * Sets the target temperature of the sensor
     * @param temp the target temperature of the sensor in degrees Celsius
     * @throws DeviceException if there was an error setting the cooler.
     */
    public abstract void setTargetTemp(double temp) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param startX the X coordinate of the top left corner of the subframe
     * @param startY the Y coordinate of the top left corner of the subframe
     * @param width the width of the subframe
     * @param height the height of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract void setSubframe(int startX, int startY, int width, int height) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param startX the X coordinate of the top left corner of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract void setSubframeStartX(int startX) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param startY the Y coordinate of the top left corner of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract void setSubframeStartY(int startY) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param width the width of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract void setSubframeWidth(int width) throws DeviceException;

    /**
     * Sets the subframe of the camera. The subframe is defined by the top left corner and the width and height of the subframe.
     * @param height the height of the subframe
     * @throws DeviceException if there was an error setting the subframe.
     */
    public abstract void setSubframeHeight(int height) throws DeviceException;

    /**
     * Sets the binning of the camera. If the camera does not support asymetric binning, binX will be applied to both axes.
     * @param binX the binning in the X direction
     * @param binY the binning in the Y direction
     * @throws DeviceException if there was an error setting the binning or the sensor does not support the requested binning.
     */
    public abstract void setBinning(int binX, int binY) throws DeviceException;

    /**
     * Sets symetrical binning of the camera.
     * @param bin the binning in both directions
     * @throws DeviceException if there was an error setting the binning or the sensor does not support the requested binning.
     */
    public abstract void setBinning(int bin) throws DeviceException;

    /**
     * Starts an exposure.
     * @param duration the exposure time in seconds
     * @param useLightFrame true to use a light frame, false to use a dark frame
     * @throws DeviceException if there was an error starting the exposure.
     */
    public abstract void startExposure(double duration, boolean useLightFrame) throws DeviceException;

    /**
     * Starts an exposure.
     * @param duration the exposure time in seconds
     * @param useLightFrame true to use a light frame, false to use a dark frame
     * @param subframe the subframe to use for the exposure, in the format [startX, startY, width, height]
     * @param binX the binning in the X direction
     * @param binY the binning in the Y direction
     * @throws DeviceException if there was an error starting the exposure.
     */
    public void startExposure(double duration, boolean useLightFrame, int[] subframe, int binX, int binY) throws DeviceException {
        setSubframe(subframe[0], subframe[1], subframe[2], subframe[3]);
        setBinning(binX, binY);
        startExposure(duration, useLightFrame);
    }

    /**
     * Stops the current exposure early, if any. The exposure will not be discarded.
     * @throws DeviceException if there was an error stopping the exposure.
     */
    public abstract void stopExposure() throws DeviceException;

    /**
     * Aborts and discards the current exposure, if any.
     * @throws DeviceException if there was an error discarding the exposure.
     */
    public abstract void abortExposure() throws DeviceException;

    // TODO: methods to slowly warm up/cool down the sensor safely


    //#endregion
    
}
