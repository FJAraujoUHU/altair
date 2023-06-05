package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;

import reactor.core.publisher.Mono;

public abstract class TelescopeService {

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters
    
    /**
     * Returns true if the telescope is connected.
     * @return True if the telescope is connected, false otherwise.
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * Returns true if the telescope is parked.
     * @return True if the telescope is parked, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isParked() throws DeviceException;

    /**
     * Returns true if the telescope is at the designated home position.
     * @return True if the telescope is at the designated home position, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isAtHome() throws DeviceException;

    /**
     * Returns true if the telescope is currently slewing.
     * @return True if the telescope is currently slewing, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isSlewing() throws DeviceException;

    /**
     * Returns true if the telescope is currently tracking an object.
     * @return True if the telescope is currently tracking an object, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isTracking() throws DeviceException;

    /**
     * Returns the current altitude and azimuth of the telescope.
     * @return A double array containing the current altitude and azimuth of the telescope, in that order.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<double[]> getAltAz() throws DeviceException;

    /**
     * Returns the current coordinates of the telescope (Right Ascension and Declination).
     * @return A double array containing the current Right Ascension and Declination of the telescope, in that order.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<double[]> getCoordinates() throws DeviceException;

    /**
     * Returns the current local sidereal time (LST) of the telescope.
     * @return The current LST of the telescope.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Double> getSiderealTime() throws DeviceException;

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects to the telescope.
     * @throws DeviceException If there was an error connecting to the telescope.
     * @return A Mono that completes when the device has been connected.
     */
    public abstract Mono<Void> connect() throws DeviceException;

    /**
     * Disconnects from the telescope.
     * @throws DeviceException If there was an error disconnecting from the telescope.
     * @return A Mono that completes when the device has been disconnected.
     */
    public abstract Mono<Void> disconnect() throws DeviceException;

    /**
     * Parks the telescope asynchonously.
     * @throws DeviceException If there was an error parking the telescope.
     * @return A Mono that completes when the telescope has been parked.
     */
    public abstract Mono<Void> park() throws DeviceException;

    /**
     * Unparks the telescope asynchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     * @return A Mono that completes when the telescope has been unparked.
     */
    public abstract Mono<Void> unpark() throws DeviceException;

    /**
     * Sets the telescope to the designated home position asynchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     * @return A Mono that completes when the telescope has been set.
     */
    public abstract Mono<Void> findHome() throws DeviceException;

    /**
     * Slew the telescope to the designated coordinates asynchronously.
     * @param rightAscension The Right Ascension to slew to.
     * @param declination The Declination to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     * @return A Mono that completes when the slew has been ordered.
     */
    public abstract Mono<Void> slewToCoords(double rightAscension, double declination) throws DeviceException;

    /**
     * Slew the telescope to the designated coordinates synchronously, and wait for the slew to complete.
     * @param rightAscension The Right Ascension to slew to.
     * @param declination The Declination to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     * @return A Mono that completes when slew is complete.
     */
    public abstract Mono<Void> slewToCoordsAwait(double rightAscension, double declination) throws DeviceException;

    /**
     * Slew the telescope to the designated altitude and azimuth asynchronously.
     * @param altitude The altitude to slew to.
     * @param azimuth The azimuth to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     * @return A Mono that completes when the slew has been ordered.
     */
    public abstract Mono<Void> slewToAltAz(double altitude, double azimuth) throws DeviceException;

    /**
     * Slew the telescope to the designated altitude and azimuth synchronously, and wait for the slew to complete.
     * @param altitude The altitude to slew to.
     * @param azimuth The azimuth to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     * @return A Mono that completes when slew is complete.
     */
    public abstract Mono<Void> slewToAltAzAwait(double altitude, double azimuth) throws DeviceException;

    /**
     * Slew the telescope relative to its current position asynchronously.
     * @param degrees The number of degrees to slew.
     * @param direction The direction to slew in (0 = North, 1 = East, 2 = South, 3 = West).
     * @throws DeviceException If there was an error slewing the telescope.
     * @throws IllegalArgumentException If the direction is not valid.
     * @return A Mono that completes when the slew has been ordered.
     */
    public Mono<Void> slewRelative(double degrees, int direction) throws DeviceException, IllegalArgumentException {
        return getAltAz().flatMap(altAz -> {
            if (altAz == null) {
                return Mono.error(new DeviceException("Could not get current altitude and azimuth."));
            }
            switch (direction) {
                case DIRECTION_NORTH:
                    return slewToAltAz(altAz[0] + degrees, altAz[1]);
                case DIRECTION_SOUTH:
                    return slewToAltAz(altAz[0] - degrees, altAz[1]);
                case DIRECTION_EAST:
                    return slewToAltAz(altAz[0], altAz[1] + degrees);
                case DIRECTION_WEST:
                    return slewToAltAz(altAz[0], altAz[1] - degrees);
                default:
                    return Mono.error(new IllegalArgumentException("Direction must be between 0 and 3."));
            }
        });
    }

    /**
     * Slew the telescope relative to its current position synchronously, and wait for the slew to complete.
     * @param degrees The number of degrees to slew.
     * @param direction The direction to slew in (0 = North, 1 = East, 2 = South, 3 = West).
     * @throws DeviceException If there was an error slewing the telescope.
     * @throws IllegalArgumentException If the direction is not valid.
     * @return A Mono that completes when slew is complete.
     */
    public Mono<Void> slewRelativeAwait(double degrees, int direction) throws DeviceException, IllegalArgumentException {
        return getAltAz().flatMap(altAz -> {
            if (altAz == null) {
                return Mono.error(new DeviceException("Could not get current altitude and azimuth."));
            }
            switch (direction) {
                case DIRECTION_NORTH:
                    return slewToAltAzAwait(altAz[0] + degrees, altAz[1]);
                case DIRECTION_SOUTH:
                    return slewToAltAzAwait(altAz[0] - degrees, altAz[1]);
                case DIRECTION_EAST:
                    return slewToAltAzAwait(altAz[0], altAz[1] + degrees);
                case DIRECTION_WEST:
                    return slewToAltAzAwait(altAz[0], altAz[1] - degrees);
                default:
                    return Mono.error(new IllegalArgumentException("Direction must be between 0 and 3."));
            }
        });
    }

    /**
     * Aborts the current slew, if there is one.
     * @throws DeviceException If there was an error aborting the current slew.
     * @return A Mono that completes when the current operation has been aborted.
     */
    public abstract Mono<Void> abortSlew() throws DeviceException;

    /**
     * Set the state of the sidereal tracking.
     * @param tracking True to enable tracking, false to disable tracking.
     * @throws DeviceException If there was an error setting the tracking.
     * @return A Mono that completes when the tracking state has been set.
     */
    public abstract Mono<Void> setTracking(boolean tracking) throws DeviceException;

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Returns the capabilities of the device
     * @return A TelescopeCapabilities object containing the capabilities of the device
     */
    public abstract Mono<TelescopeCapabilities> getCapabilities();

    /**
     * Returns the status of the device
     * @return A TelescopeStatus object containing the status of the device
     */
    public abstract Mono<TelescopeStatus> getStatus();

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    public static final int DIRECTION_NORTH = 0;
    public static final int DIRECTION_EAST = 1;
    public static final int DIRECTION_SOUTH = 2;
    public static final int DIRECTION_WEST = 3;

    /**
     * A record containing the device capabilities
     */
    public record TelescopeCapabilities(
        boolean canFindHome,
        boolean canPark,
        boolean canUnpark,
        boolean canSlewAwait,
        boolean canSlew,
        boolean canTrack
    ) {}

    /**
     * A record containing the device status
     */
    public record TelescopeStatus(
        boolean connected,
        double altitude,
        double azimuth,
        double rightAscension,
        double declination,
        boolean atHome,
        boolean parked,
        boolean slewing,
        boolean tracking,
        double siderealTime
    ) {}

    //#endregion
}
