package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;

import reactor.core.publisher.Mono;

public abstract class DomeService {

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    /**
     * Returns true if the dome is connected.
     * @return True if the dome is connected, false otherwise.
     */
    public abstract Mono<Boolean> isConnected();

    /**
     * Returns true if the dome is parked.
     * @return True if the dome is parked, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isParked() throws DeviceException;

    /**
     * Returns true if the dome is at the designated home position.
     * @return True if the dome is at the designated home position, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isAtHome() throws DeviceException;

    /**
     * Returns true if the dome is currently slewing.
     * @return True if the dome is currently slewing, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isSlewing() throws DeviceException;

    /**
     * Returns true if the dome is currently slaved to the telescope.
     * @return True if the dome is currently slaved to the telescope, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isSlaved() throws DeviceException;

    /**
     * Returns true if the shutter is open. Note that if the shutter is able to change
     * altitude and it is set to 0, it still counts as open.
     * @return True if the shutter is open, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isShutterOpen() throws DeviceException;

    /**
     * Returns the azimuth the dome is pointing at.
     * @return The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Double> getAz() throws DeviceException;

    /**
     * Returns the altitude the shutter is set to.
     * @return The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Double> getAlt() throws DeviceException;

    /**
     * Returns the azimuth and altitude the dome is pointing at.
     * @return The azimuth and altitude the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error polling the data.
     */
    public Mono<double[]> getAltAz() throws DeviceException {
        return Mono.zip(getAz(), getAlt()).map(tuple -> new double[] {tuple.getT1(), tuple.getT2()});
    }

    /**
     * Returns the status of the shutter.
     * @return The status of the shutter, one of the SHUTTER_* constants.
     * @throws DeviceException If there was an error polling the data.
     * @see #SHUTTER_OPEN
     */
    public abstract Mono<Integer> getShutterStatus() throws DeviceException;

    /**
     * Returns the shutter position as a fraction of the total range.
     * @return The shutter position as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error polling the data.
     */
    public Mono<Double> getShutter() throws DeviceException {
        return getAlt().map(alt -> alt/90.0);
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects to the dome.
     * @throws DeviceException If there was an error connecting to the dome.
     */
    public abstract Mono<Void> connect() throws DeviceException;

    /**
     * Disconnects from the dome.
     * @throws DeviceException If there was an error disconnecting from the dome.
     */
    public abstract Mono<Void> disconnect() throws DeviceException;

    /**
     * Opens the shutter asynchronously.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract Mono<Void> openShutter() throws DeviceException;

    /**
     * Opens the shutter.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract Mono<Void> openShutterAwait() throws DeviceException;

    /**
     * Closes the shutter asynchronously.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract Mono<Void> closeShutter() throws DeviceException;

    /**
     * Closes the shutter.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract Mono<Void> closeShutterAwait() throws DeviceException;

    /**
     * Sets the shutter to the specified position asynchronously.
     * @param position The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public Mono<Void> setShutter(double position) throws DeviceException {
        if (position < 0) {
            position = 0;
        }
        if (position > 1) {
            position = 1;
        }
        return setAlt(position*90.0);
    } 

    /**
     * Sets the shutter to the specified position.
     * @param position The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public Mono<Void> setShutterAwait(double position) throws DeviceException {
        if (position < 0) {
            position = 0;
        }
        if (position > 1) {
            position = 1;
        }
        return setAltAwait(position*90.0);
    }

    /**
     * Sets the shutter to the specified position relative to its current position asynchronously.
     * @param rate The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public Mono<Void> setShutterRelative(double rate) throws DeviceException {
        return getShutter().flatMap(oldShutter -> {
            double newShutter = oldShutter + rate;
            if (newShutter < 0) {
                newShutter = 0;
            }
            if (newShutter > 1) {
                newShutter = 1;
            }

            return setShutter(newShutter);
        });
    }

    /**
     * Sets the shutter to the specified position relative to its current position.
     * @param rate The position to set the shutter to, as a fraction of the total range, positive opening the shutter and negative closing it.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public Mono<Void> setShutterRelativeAwait(double rate) throws DeviceException {
        return getShutter().flatMap(oldShutter -> {
            double newShutter = oldShutter + rate;
            if (newShutter < 0) {
                newShutter = 0;
            }
            if (newShutter > 1) {
                newShutter = 1;
            }
    
            return setShutterAwait(newShutter);
        });        
    }

    /**
     * Sets the altitude the shutter is set to asynchronously.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract Mono<Void> setAlt(double degrees) throws DeviceException;
    
    /**
     * Sets the altitude the shutter is set to.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract Mono<Void> setAltAwait(double degrees) throws DeviceException;

    /**
     * Sets the altitude the shutter is set to relative to its current position asynchronously.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public Mono<Void> setAltRelative(double degreesDelta) throws DeviceException {
        return getAlt().flatMap(oldAlt -> {
            double newAlt = oldAlt + degreesDelta;
            if (newAlt < 0) {
                newAlt = 0;
            }
            if (newAlt > 90) {
                newAlt = 90;
            }
            return setAlt(newAlt);
        });
    }

    /**
     * Sets the altitude the shutter is set to relative to its current position.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public Mono<Void> setAltRelativeAwait(double degreesDelta) throws DeviceException {
        return getAlt().flatMap(oldAlt -> {
            double newAlt = oldAlt + degreesDelta;
            if (newAlt < 0) {
                newAlt = 0;
            }
            if (newAlt > 90) {
                newAlt = 90;
            }
            return setAltAwait(newAlt);
        });
    }

    /**
     * Sets the azimuth the dome is pointing at asynchronously.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract Mono<Void> slew(double az) throws DeviceException;

    /**
     * Sets the azimuth the dome is pointing at.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract Mono<Void> slewAwait(double az) throws DeviceException;

    /**
     * Slew the dome relative to its current position asynchronously.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public Mono<Void> slewRelative(double degrees) throws DeviceException {
        return getAz().flatMap(azimuth -> {
            double newAzimuth = azimuth + degrees;
            newAzimuth = newAzimuth % 360; // Wrap around
            return slew(newAzimuth);
        });
    }

    /**
     * Slew the dome relative to its current position.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public Mono<Void> slewRelativeAwait(double degrees) throws DeviceException {
        return getAz().flatMap(currentAz -> {
            double newAzimuth = currentAz + degrees;
            newAzimuth = newAzimuth % 360; // Wrap around
            return slewAwait(newAzimuth);
        });
    }

    /**
     * Parks the dome asynchronously. Note that parking does not involve closing the shutter.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract Mono<Void> park() throws DeviceException;

    /**
     * Sends a signal to park, and waits until it does. Note that parking does not involve closing the shutter.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract Mono<Void> parkAwait() throws DeviceException;

    /**
     * Unparks the dome asynchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract Mono<Void> unpark() throws DeviceException;

    /**
     * Unparks the dome synchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract Mono<Void> unparkAwait() throws DeviceException;

    /**
     * Sets if the dome is slaved to the telescope.
     * @throws DeviceException If there was an error slaving the dome.
     */
    public abstract Mono<Void> setSlaved(boolean slaved) throws DeviceException;

    /**
     * Sets the dome to the designated home position asynchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract Mono<Void> findHome() throws DeviceException;

    /**
     * Sets the dome to the designated home position, and waits until it reaches it.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract Mono<Void> findHomeAwait() throws DeviceException;

    /**
     * Halts the dome. This should stop the dome from rotating, the shutter from moving, and should
     * disengage slaving.
     * @throws DeviceException If there was an error halting the dome.
     */
    public abstract Mono<Void> halt() throws DeviceException;

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Returns the capabilities of the device
     * @return A DomeCapabilities object containing the capabilities of the device
     */
    public abstract Mono<DomeCapabilities> getCapabilities();

    /**
     * Returns the status of the device
     * @return A DomeStatus object containing the status of the device
     */
    public abstract Mono<DomeStatus> getStatus();
    

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    public static final int SHUTTER_OPEN = 0;
    public static final int SHUTTER_CLOSED = 1;
    public static final int SHUTTER_OPENING = 2;
    public static final int SHUTTER_CLOSING = 3;
    public static final int SHUTTER_ERROR = 4;

    /**
     * A record containing the device capabilities
     */
    public record DomeCapabilities(
        boolean canFindHome,
        boolean canPark,
        boolean canUnpark,
        boolean canShutter,
        boolean canSetAzimuth,
        boolean canSetAltitude,
        boolean canSlave
    ) {}

    /**
     * A record containing the device status
     */
    public record DomeStatus(
        boolean connected,
        double azimuth,
        int shutter,
        String shutterStatus,
        boolean atHome,
        boolean parked,
        boolean slewing,
        boolean slaved   
    ) {}



}
