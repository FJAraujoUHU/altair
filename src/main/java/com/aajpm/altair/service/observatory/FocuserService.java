package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.statusreporting.FocuserStatus;

import reactor.core.publisher.Mono;

public abstract class FocuserService {
    
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

    /**
     * Returns the status of the focuser
     * @return A FocuserStatus object containing the status of the focuser
     * @throws DeviceException If there was an error polling the data.
     */
    public Mono<FocuserStatus> getStatus() throws DeviceException {
        Mono<Boolean> connected = isConnected();
        Mono<Integer> position = getPosition();
        Mono<Double> temperature = getTemperature();
        Mono<Boolean> tempComp = isTempComp();
        Mono<Boolean> moving = isMoving();

        return Mono.zip(connected, position, temperature, tempComp, moving)
            .map(tuple -> {
                FocuserStatus focuserStatus = new FocuserStatus();
                focuserStatus.setConnected(tuple.getT1());
                focuserStatus.setPosition(tuple.getT2());
                focuserStatus.setTemperature(tuple.getT3());
                focuserStatus.setTempComp(tuple.getT4());
                focuserStatus.setMoving(tuple.getT5());
                return focuserStatus;
            }).onErrorReturn(FocuserStatus.getErrorStatus());
    }

    //#endregion Getters
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects the focuser
     * @throws DeviceException If there was an error connecting the focuser.
     */
     public abstract void connect() throws DeviceException;

    /**
     * Disconnects the focuser
     * @throws DeviceException If there was an error disconnecting the focuser.
     */
    public abstract void disconnect() throws DeviceException;

    /**
     * Moves the focuser to the specified absolute position asynchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public abstract void move(int position) throws DeviceException;

    /**
     * Moves the focuser to the specified absolute position synchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public abstract void moveAwait(int position) throws DeviceException;

    /**
     * Moves the focuser relative to the current position asynchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public void moveRelative(int position) throws DeviceException {
        getPosition().subscribe(currentPosition -> {
            int newPosition = currentPosition + position;
            newPosition = Math.max(newPosition, 0); // Clamp to 0
            move(newPosition);
        });
    }

    /**
     * Moves the focuser relative to the current position synchronously
     * @param position The position to move to, in steps
     * @throws DeviceException If there was an error moving the focuser.
     */
    public void moveRelativeAwait(int position) throws DeviceException {
            int currentPosition = getPosition().block();    
            int newPosition = currentPosition + position;
            newPosition = Math.max(newPosition, 0); // Clamp to 0
            moveAwait(newPosition);
    }

    /**
     * Inmediately stops the focuser
     * @throws DeviceException If there was an error halting the focuser.
     */
    public abstract void halt() throws DeviceException;

    /**
     * Sets the temperature-compensation state of the focuser
     * @param tempComp TRUE to enable temperature-compensation, FALSE to disable
     * @throws DeviceException If there was an error setting the temperature-compensation state or the device does not support temperature-compensation.
     */
    public abstract void setTempComp(boolean enable) throws DeviceException;

    //#endregion Setters
}
