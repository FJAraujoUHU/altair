package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.statusreporting.TelescopeStatus;

public abstract class TelescopeService {

    /////////////////////////////// CONSTANTS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Constants">

    public static final int DIRECTION_NORTH = 0;
    public static final int DIRECTION_EAST = 1;
    public static final int DIRECTION_SOUTH = 2;
    public static final int DIRECTION_WEST = 3;
    
    //</editor-fold>
    ///////////////////////////////// GETTERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Getters">
    
    /**
     * Returns true if the telescope is connected.
     * @return True if the telescope is connected, false otherwise.
     */
    public abstract boolean isConnected();

    /**
     * Returns true if the telescope is parked.
     * @return True if the telescope is parked, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isParked() throws DeviceException;

    /**
     * Returns true if the telescope is at the designated home position.
     * @return True if the telescope is at the designated home position, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isAtHome() throws DeviceException;

    /**
     * Returns true if the telescope is currently slewing.
     * @return True if the telescope is currently slewing, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isSlewing() throws DeviceException;

    /**
     * Returns true if the telescope is currently tracking an object.
     * @return True if the telescope is currently tracking an object, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isTracking() throws DeviceException;

    /**
     * Returns the current altitude and azimuth of the telescope.
     * @return A double array containing the current altitude and azimuth of the telescope, in that order.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract double[] getAltAz() throws DeviceException;

    /**
     * Returns the current coordinates of the telescope (Right Ascension and Declination).
     * @return A double array containing the current Right Ascension and Declination of the telescope, in that order.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract double[] getCoordinates() throws DeviceException;

    /**
     * Returns the current local sidereal time (LST) of the telescope.
     * @return The current LST of the telescope.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract double getSiderealTime() throws DeviceException;

    /**
     * Returns the current status of the telescope.
     * @return A POJO containing the current status of the telescope.
     * @throws DeviceException If there was an error polling the data.
     */
    public TelescopeStatus getStatus() throws DeviceException {
        TelescopeStatus status = new TelescopeStatus();
        status.setConnected(isConnected());
        double[] altAz = getAltAz();
        status.setAltitude(altAz[0]);
        status.setAzimuth(altAz[1]);
        double[] coordinates = getCoordinates();
        status.setRightAscension(coordinates[0]);
        status.setDeclination(coordinates[1]);
        status.setAtHome(isAtHome());
        status.setParked(isParked());
        status.setSlewing(isSlewing());
        status.setTracking(isTracking());
        status.setSiderealTime(getSiderealTime());

        return status;
    }


    //</editor-fold>
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Setters/Actions">

    /**
     * Connects to the telescope.
     * @throws DeviceException If there was an error connecting to the telescope.
     */
    public abstract void connect() throws DeviceException;

    /**
     * Disconnects from the telescope.
     * @throws DeviceException If there was an error disconnecting from the telescope.
     */
    public abstract void disconnect() throws DeviceException;

    /**
     * Parks the telescope synchonously.
     * @throws DeviceException If there was an error parking the telescope.
     */
    public abstract void park() throws DeviceException;

    /**
     * Parks the telescope asynchonously.
     * @throws DeviceException If there was an error parking the telescope.
     */
    public abstract void parkAsync() throws DeviceException;

    /**
     * Unparks the telescope.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract void unpark() throws DeviceException;

    /**
     * Unparks the telescope asynchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract void unparkAsync() throws DeviceException;

    /**
     * Sets the telescope to the designated home position synchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract void findHome() throws DeviceException;

    /**
     * Sets the telescope to the designated home position asynchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract void findHomeAsync() throws DeviceException;

    /**
     * Slew the telescope to the designated coordinates synchronously.
     * @param ra The Right Ascension to slew to.
     * @param dec The Declination to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     */
    public abstract void slewToCoords(double ra, double dec) throws DeviceException;

    /**
     * Slew the telescope to the designated coordinates asynchronously.
     * @param ra The Right Ascension to slew to.
     * @param dec The Declination to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     */
    public abstract void slewToCoordsAsync(double rightAscension, double declination) throws DeviceException;

    /**
     * Slew the telescope to the designated altitude and azimuth synchronously.
     * @param altitude The altitude to slew to.
     * @param azimuth The azimuth to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     */
    public abstract void slewToAltAz(double altitude, double azimuth) throws DeviceException;

    /**
     * Slew the telescope to the designated altitude and azimuth asynchronously.
     * @param altitude The altitude to slew to.
     * @param azimuth The azimuth to slew to.
     * @throws DeviceException If there was an error slewing the telescope.
     */
    public abstract void slewToAltAzAsync(double altitude, double azimuth) throws DeviceException;

    /**
     * Slew the telescope relative to its current position synchronously.
     * @param degrees The number of degrees to slew.
     * @param direction The direction to slew in (0 = North, 1 = East, 2 = South, 3 = West).
     * @throws DeviceException If there was an error slewing the telescope.
     * @throws IllegalArgumentException If the direction is not valid.
     */
    public void slewRelative(double degrees, int direction) throws DeviceException, IllegalArgumentException {
        double[] altAz = getAltAz();
        switch (direction) {
            case DIRECTION_NORTH:
                slewToAltAz(altAz[0] + degrees, altAz[1]);
                break;
            case DIRECTION_SOUTH:
                slewToAltAz(altAz[0] - degrees, altAz[1]);
                break;
            case DIRECTION_EAST:
                slewToAltAz(altAz[0], altAz[1] + degrees);
                break;
            case DIRECTION_WEST:
                slewToAltAz(altAz[0], altAz[1] - degrees);
                break;
            default:
                throw new IllegalArgumentException("Direction must be between 0 and 3.");
        }
    }

    /**
     * Slew the telescope relative to its current position asynchronously.
     * @param degrees The number of degrees to slew.
     * @param direction The direction to slew in (0 = North, 1 = East, 2 = South, 3 = West).
     * @throws DeviceException If there was an error slewing the telescope.
     * @throws IllegalArgumentException If the direction is not valid.
     */
    public void slewRelativeAsync(double degrees, int direction) throws DeviceException, IllegalArgumentException {
        double[] altAz = getAltAz();
        switch (direction) {
            case DIRECTION_NORTH:
                slewToAltAzAsync(altAz[0] + degrees, altAz[1]);
                break;
            case DIRECTION_SOUTH:
                slewToAltAzAsync(altAz[0] - degrees, altAz[1]);
                break;
            case DIRECTION_EAST:
                slewToAltAzAsync(altAz[0], altAz[1] + degrees);
                break;
            case DIRECTION_WEST:
                slewToAltAzAsync(altAz[0], altAz[1] - degrees);
                break;
            default:
                throw new IllegalArgumentException("Direction must be between 0 and 3.");
        }
    }

    /**
     * Aborts the current slew, if there is one.
     * @throws DeviceException If there was an error aborting the current slew.
     */
    public abstract void abortSlew() throws DeviceException;

    /**
     * Set the state of the sidereal tracking.
     * @param tracking True to enable tracking, false to disable tracking.
     * @throws DeviceException If there was an error setting the tracking.
     */
    public abstract void setTracking(boolean tracking) throws DeviceException;

    //</editor-fold>
}