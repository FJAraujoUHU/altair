package com.aajpm.altair.service.observatory;

import java.util.List;

import com.aajpm.altair.config.ObservatoryConfig.FilterWheelConfig;
import com.aajpm.altair.utility.exception.DeviceException;

import reactor.core.publisher.Mono;

public abstract class FilterWheelService {

    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    protected FilterWheelConfig config;

    //#endregion
    ////////////////////////////// CONSTRUCTOR /////////////////////////////////
    //#region Constructor

    /**
     * Creates a new instance of the FilterWheelService class
     */
    protected FilterWheelService(FilterWheelConfig config) {
        this.config = config;
    }

    //#endregion
    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    /**
     * Returns whether the filter wheel is connected
     * @return TRUE if connected, FALSE otherwise
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * Returns whether the filter wheel is moving or stationary
     * @return TRUE if moving, FALSE otherwise
     * @throws DeviceException
     */
    public abstract Mono<Boolean> isMoving() throws DeviceException;

    /**
     * Returns the current position of the filter wheel
     * @return The current selected filter, as a zero-based index. If the filter wheel is moving, returns the filter it is moving to.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Integer> getPosition() throws DeviceException;

    /**
     * Returns the name of the current filter
     * @return The name of the current filter
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<String> getFilterName() throws DeviceException;

    /**
     * Returns the focus offset of the current filter
     * @return The focus offset of the current filter, in steps
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Integer> getFocusOffset() throws DeviceException;

    /**
     * Returns the number of filters in the filter wheel
     * @return The number of filters in the filter wheel
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Integer> getFilterCount() throws DeviceException;

    /**
     * Returns the names of all installed filters
     * @return A list of all installed filters as strings.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<List<String>> getFilterNames() throws DeviceException;

    /**
     * Returns the focus offsets of all installed filters
     * @return A list of all installed filters' focus offsets, in steps.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<List<Integer>> getFocusOffsets() throws DeviceException;

    
    //#endregion
    ///////////////////////////////// SETTERS /////////////////////////////////
    //#region Setters

    /**
     * Connects the filter wheel
     * @throws DeviceException If there was an error connecting the filter wheel.
     */
    public abstract Mono<Boolean> connect() throws DeviceException;

    /**
     * Disconnects the filter wheel
     * @throws DeviceException If there was an error disconnecting the filter wheel.
     */
    public abstract Mono<Boolean> disconnect() throws DeviceException;

    /**
     * Moves the filter wheel to the specified position
     * @param position The position to move to, as a zero-based index
     * @throws DeviceException If there was an error moving the filter wheel.
     */
    public abstract Mono<Boolean> setPosition(int position) throws DeviceException;

    /**
     * Moves the filter wheel to the specified position synchronously, and wait for the operation to complete
     * @param position The position to move to, as a zero-based index
     * @throws DeviceException If there was an error moving the filter wheel.
     */
    public abstract Mono<Boolean> setPositionAwait(int position) throws DeviceException;

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status reporting

    public abstract Mono<FilterWheelStatus> getStatus();

    public record FilterWheelStatus (
        Boolean connected,
        Integer curPosition,
        String curName,
        Integer curOffset,
        Boolean isMoving
    ) {}

    //#endregion

}
