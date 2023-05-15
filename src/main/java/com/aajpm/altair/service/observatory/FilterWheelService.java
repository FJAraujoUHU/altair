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
     * Returns the current position of the filter wheel
     * @return The current selected filter, as a zero-based index
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
    public abstract void connect() throws DeviceException;

    /**
     * Disconnects the filter wheel
     * @throws DeviceException If there was an error disconnecting the filter wheel.
     */
    public abstract void disconnect() throws DeviceException;

    /**
     * Moves the filter wheel to the specified position
     * @param position The position to move to, as a zero-based index
     * @throws DeviceException If there was an error moving the filter wheel.
     */
    public abstract void setPosition(int position) throws DeviceException;

    /**
     * Moves the filter wheel to the specified position synchronously
     * @param position The position to move to, as a zero-based index
     * @throws DeviceException If there was an error moving the filter wheel.
     */
    public abstract void setPositionAwait(int position) throws DeviceException;

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status reporting

    public Mono<FilterWheelStatus> getStatus() {
        Mono<Boolean> connected =   this.isConnected().onErrorReturn(false);
        Mono<Integer> pos =         this.getPosition().onErrorReturn(-1);
        Mono<String> name =         this.getFilterName().onErrorReturn("Unknown");
        Mono<Integer> offset =      this.getFocusOffset().onErrorReturn(0);

        return Mono.zip(connected, pos, name, offset).map(
            tuple -> new FilterWheelStatus(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
            )
        );
    }

    public record FilterWheelStatus (
        Boolean connected,
        Integer curPosition,
        String curName,
        Integer curOffset
    ) {}

    //#endregion

}
