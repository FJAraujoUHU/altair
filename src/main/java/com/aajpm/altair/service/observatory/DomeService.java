package com.aajpm.altair.service.observatory;

import com.aajpm.altair.config.ObservatoryConfig.DomeConfig;
import com.aajpm.altair.utility.exception.DeviceException;

import reactor.core.publisher.Mono;

public abstract class DomeService {
    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    protected DomeConfig config;

    //#endregion
    ////////////////////////////// CONSTRUCTOR /////////////////////////////////
    //#region Constructor

    /**
     * Creates a new instance of the DomeService class
     */
    protected DomeService(DomeConfig config) {
        this.config = config;
    }

    //#endregion
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
     * 
     * A slaved dome will automatically move to keep the telescope in view in
     * both altitude and azimuth.
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
     * Returns true if the shutter is closed. Note that if the shutter is able to change
     * altitude and it is set to 0, it still counts as open.
     * @return True if the shutter is closed, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract Mono<Boolean> isShutterClosed() throws DeviceException;

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
    public abstract Mono<Boolean> connect() throws DeviceException;

    /**
     * Disconnects from the dome.
     * @throws DeviceException If there was an error disconnecting from the dome.
     */
    public abstract Mono<Boolean> disconnect() throws DeviceException;

    /**
     * Opens the shutter asynchronously.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract Mono<Boolean> openShutter() throws DeviceException;

    /**
     * Opens the shutter.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract Mono<Boolean> openShutterAwait() throws DeviceException;

    /**
     * Closes the shutter asynchronously.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract Mono<Boolean> closeShutter() throws DeviceException;

    /**
     * Closes the shutter.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract Mono<Boolean> closeShutterAwait() throws DeviceException;

    /**
     * Sets the shutter to the specified position asynchronously.
     * @param position The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public Mono<Boolean> setShutter(double position) throws DeviceException {
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
    public Mono<Boolean> setShutterAwait(double position) throws DeviceException {
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
    public Mono<Boolean> setShutterRelative(double rate) throws DeviceException {
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
    public Mono<Boolean> setShutterRelativeAwait(double rate) throws DeviceException {
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
    public abstract Mono<Boolean> setAlt(double degrees) throws DeviceException;
    
    /**
     * Sets the altitude the shutter is set to.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract Mono<Boolean> setAltAwait(double degrees) throws DeviceException;

    /**
     * Sets the altitude the shutter is set to relative to its current position asynchronously.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public Mono<Boolean> setAltRelative(double degreesDelta) throws DeviceException {
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
    public Mono<Boolean> setAltRelativeAwait(double degreesDelta) throws DeviceException {
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
    public abstract Mono<Boolean> slew(double az) throws DeviceException;

    /**
     * Sets the azimuth the dome is pointing at.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract Mono<Boolean> slewAwait(double az) throws DeviceException;

    /**
     * Slew the dome relative to its current position asynchronously.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public Mono<Boolean> slewRelative(double degrees) throws DeviceException {
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
    public Mono<Boolean> slewRelativeAwait(double degrees) throws DeviceException {
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
    public abstract Mono<Boolean> park() throws DeviceException;

    /**
     * Sends a signal to park, and waits until it does. Note that parking does not involve closing the shutter.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract Mono<Boolean> parkAwait() throws DeviceException;

    /**
     * Unparks the dome asynchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract Mono<Boolean> unpark() throws DeviceException;

    /**
     * Unparks the dome synchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract Mono<Boolean> unparkAwait() throws DeviceException;

    /**
     * Sets if the dome is slaved to the telescope.
     * 
     * A slaved dome will automatically move to keep the telescope in view in
     * both altitude and azimuth.
     * @throws DeviceException If there was an error slaving the dome.
     */
    public abstract Mono<Boolean> setSlaved(boolean slaved) throws DeviceException;

    /**
     * Sets the dome to the designated home position asynchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract Mono<Boolean> findHome() throws DeviceException;

    /**
     * Sets the dome to the designated home position, and waits until it reaches it.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract Mono<Boolean> findHomeAwait() throws DeviceException;

    /**
     * Halts the dome. This should stop the dome from rotating, the shutter from moving, and should
     * disengage slaving.
     * @throws DeviceException If there was an error halting the dome.
     */
    public abstract Mono<Boolean> halt() throws DeviceException;

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

    public static final String SHUTTER_CLOSED_STATUS = "Closed";
    public static final String SHUTTER_OPEN_STATUS = "Open";
    public static final String SHUTTER_OPENING_STATUS = "Opening";
    public static final String SHUTTER_CLOSING_STATUS = "Closing";
    public static final String SHUTTER_ERROR_STATUS = "Error";

    protected static String shutterStatusToString(Integer shutterStatus) {
        switch (shutterStatus) {
            case SHUTTER_OPEN:
                return SHUTTER_OPEN_STATUS;
            case SHUTTER_CLOSED:
                return SHUTTER_CLOSED_STATUS;
            case SHUTTER_OPENING:
                return SHUTTER_OPENING_STATUS;
            case SHUTTER_CLOSING:
                return SHUTTER_CLOSING_STATUS;
            default:
                return SHUTTER_ERROR_STATUS;
        }
    }

    

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
        boolean canSlave,
        /** 
         * In the rare case the dome, when finding home or parking,
         * it will comply, but won't always correctly report it in isAtHome()
         * and isParked(). This seems to be the case for the MaxDome II ASCOM
         * driver.
         */
        boolean isNaughty
    ) {}

    /**
     * A record containing the device status
     */
    public record DomeStatus(
        /** If the dome is connected */
        boolean connected,
        /** The azimuth the dome is aiming at */
        double azimuth,
        /** How open is the shutter opened in a scale of 0-100 */
        int shutter,
        /** The status of the shutter as a String */
        String shutterStatus,
        /** If the dome is at its home position */
        boolean atHome,
        /** If the dome is parked */
        boolean parked,
        /** If the dome is slewing */
        boolean slewing,
        /** If the dome is slaved to the telescope */
        boolean slaved   
    ) {}



}
