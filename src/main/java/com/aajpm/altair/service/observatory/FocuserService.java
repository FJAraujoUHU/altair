package com.aajpm.altair.service.observatory;

import com.aajpm.altair.config.ObservatoryConfig.FocuserConfig;
import com.aajpm.altair.utility.exception.DeviceException;

import reactor.core.publisher.Mono;

public abstract class FocuserService {
    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    protected FocuserConfig config;

    //#endregion
    ////////////////////////////// CONSTRUCTOR /////////////////////////////////
    //#region Constructor

    /**
     * Creates a new instance of the FocuserService class
     */
    protected FocuserService(FocuserConfig config) {
        this.config = config;
    }

    //#endregion
    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    /**
     * Returns whether the focuser is connected
     * @return TRUE if connected, FALSE otherwise
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * Returns the current position of the focuser
     * @return The position of the focuser, in steps.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Integer> getPosition() throws DeviceException;

    /**
     * Returns the current temperature of the focuser
     * @return The temperature of the focuser, in degrees Celsius
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Double> getTemperature() throws DeviceException;

    /**
     * Returns whether the focuser is currently temperature-compensating
     * @return TRUE if temperature-compensating, FALSE otherwise
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isTempComp() throws DeviceException;

    /**
     * Returns whether the focuser is currently moving
     * @return TRUE if moving, FALSE otherwise
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isMoving() throws DeviceException;


    //#endregion Getters
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects the focuser
     * @throws DeviceException If there was an error connecting the focuser.
     */
     public abstract Mono<Boolean> connect() throws DeviceException;

    /**
     * Disconnects the focuser
     * @throws DeviceException If there was an error disconnecting the focuser.
     */
    public abstract Mono<Boolean> disconnect() throws DeviceException;

    /**
     * Moves the focuser to the specified absolute position asynchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public abstract Mono<Boolean> move(int position) throws DeviceException;

    /**
     * Moves the focuser to the specified absolute position synchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public abstract Mono<Boolean> moveAwait(int position) throws DeviceException;

    /**
     * Moves the focuser relative to the current position asynchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public abstract Mono<Boolean> moveRelative(int position) throws DeviceException;

    /**
     * Moves the focuser relative to the current position synchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public abstract Mono<Boolean> moveRelativeAwait(int position) throws DeviceException;

    /**
     * Inmediately stops the focuser
     * @throws DeviceException If there was an error halting the focuser.
     */
    public abstract Mono<Boolean> halt() throws DeviceException;

    /**
     * Sets the temperature-compensation state of the focuser
     * @param tempComp TRUE to enable temperature-compensation, FALSE to disable
     * @throws DeviceException If there was an error setting the temperature-compensation state or the device does not support temperature-compensation.
     */
    public abstract Mono<Boolean> setTempComp(boolean enable) throws DeviceException;

    //#endregion Setters
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Returns the capabilities of the device
     * @return A FocuserCapabilities object containing the capabilities of the device
     */
    public abstract Mono<FocuserCapabilities> getCapabilities();

    /**
     * Returns the status of the focuser
     * @return A FocuserStatus object containing the status of the focuser
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<FocuserStatus> getStatus() throws DeviceException;
    

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    /**
     * A record containing the device capabilities
     */
    public record FocuserCapabilities(
        /** If the device can move to an absolute position (this is transparent to the user, implementations must handle this) */
        boolean canAbsolute,
        /** If the device supports temperature compensation */
        boolean canTempComp,
        /** The maximum increment the device can move in a single move operation */
        int maxIncrement,
        /**  The maximum step the device can move to, that is, the range of the device is [0-maxStep] */
        int maxStep,
        /** The physical size of a single step, in microns */
        int stepSize
    ) {}

    /**
     * A record containing the device status
     */
    public record FocuserStatus(
        boolean connected,
        int position,
        double temperature,
        boolean tempComp,
        boolean moving
    ) {}

    //#endregion
}
