package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.statusreporting.DomeStatus;

public abstract class DomeService {


    /////////////////////////////// CONSTANTS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Constants">

    public static final int SHUTTER_OPEN = 0;
    public static final int SHUTTER_CLOSED = 1;
    public static final int SHUTTER_OPENING = 2;
    public static final int SHUTTER_CLOSING = 3;
    public static final int SHUTTER_ERROR = 4;

    //</editor-fold>
    ///////////////////////////////// GETTERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Getters">

    /**
     * Returns true if the dome is connected.
     * @return True if the dome is connected, false otherwise.
     */
    public abstract boolean isConnected();

    /**
     * Returns true if the dome is parked.
     * @return True if the dome is parked, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isParked() throws DeviceException;

    /**
     * Returns true if the dome is at the designated home position.
     * @return True if the dome is at the designated home position, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isAtHome() throws DeviceException;

    /**
     * Returns true if the dome is currently slewing.
     * @return True if the dome is currently slewing, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isSlewing() throws DeviceException;

    /**
     * Returns true if the dome is currently slaved to the telescope.
     * @return True if the dome is currently slaved to the telescope, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isSlaved() throws DeviceException;

    /**
     * Returns true if the shutter is open. Note that if the shutter is able to change
     * altitude and it is set to 0, it still counts as open.
     * @return True if the shutter is open, false otherwise.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract boolean isShutterOpen() throws DeviceException;

    /**
     * Returns the azimuth the dome is pointing at.
     * @return The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract double getAz() throws DeviceException;

    /**
     * Returns the altitude the shutter is set to.
     * @return The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error polling the data.
     */
    public abstract double getAlt() throws DeviceException;

    /**
     * Returns the status of the shutter.
     * @return The status of the shutter, one of the SHUTTER_* constants.
     * @throws DeviceException If there was an error polling the data.
     * @see #SHUTTER_OPEN
     */
    public abstract int getShutterStatus() throws DeviceException;

    /**
     * Returns the shutter position as a fraction of the total range.
     * @return The shutter position as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error polling the data.
     */
    public double getShutter() throws DeviceException {
        return getAlt()/90.0;
    }

    /**
     * Returns the current status of the dome.
     * @return A POJO containing the current status of the dome.
     * @throws DeviceException If there was an error polling the data.
     */
    public DomeStatus getDomeStatus() throws DeviceException {
        DomeStatus status = new DomeStatus();
        status.setConnected(isConnected());
        status.setAzimuth(getAz());
        status.setShutter((int) (getShutter()*100));
        switch (getShutterStatus()) {
            case SHUTTER_OPEN:
                status.setShutterStatus("Open");
                break;
            case SHUTTER_CLOSED:
                status.setShutterStatus("Closed");
                break;
            case SHUTTER_OPENING:
                status.setShutterStatus("Opening");
                break;
            case SHUTTER_CLOSING:
                status.setShutterStatus("Closing");
                break;
            default:
                status.setShutterStatus("Error");
                break;
        }
        status.setAtHome(isAtHome());
        status.setParked(isParked());
        status.setSlaved(isSlaved());
        status.setSlewing(isSlewing());

        return status;
        
    }

    //</editor-fold>
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Setters/Actions">

    /**
     * Connects to the dome.
     * @throws DeviceException If there was an error connecting to the dome.
     */
    public abstract void connect() throws DeviceException;

    /**
     * Disconnects from the dome.
     * @throws DeviceException If there was an error disconnecting from the dome.
     */
    public abstract void disconnect() throws DeviceException;

    /**
     * Opens the shutter.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract void openShutter() throws DeviceException;

    /**
     * Opens the shutter asynchronously.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract void openShutterAsync() throws DeviceException;

    /**
     * Closes the shutter.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract void closeShutter() throws DeviceException;

    /**
     * Closes the shutter asynchronously.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract void closeShutterAsync() throws DeviceException;

    /**
     * Sets the shutter to the specified position.
     * @param position The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutter(double position) throws DeviceException {
        if (position < 0) {
            position = 0;
        }
        if (position > 1) {
            position = 1;
        }
        setAlt(position*90.0);
    }

    /**
     * Sets the shutter to the specified position relative to its current position.
     * @param rate The position to set the shutter to, as a fraction of the total range, positive opening the shutter and negative closing it.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutterRelative(double rate) throws DeviceException {
        double oldShutter = getShutter();
        double newShutter = oldShutter + rate;
        if (newShutter < 0) {
            newShutter = 0;
        }
        if (newShutter > 1) {
            newShutter = 1;
        }

        setShutter(newShutter);
    }

    /**
     * Sets the shutter to the specified position relative to its current position asynchronously.
     * @param rate The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutterRelativeAsync(double rate) throws DeviceException {
        double oldShutter = getShutter();
        double newShutter = oldShutter + rate;
        if (newShutter < 0) {
            newShutter = 0;
        }
        if (newShutter > 1) {
            newShutter = 1;
        }

        setShutterAsync(newShutter);
    }

    /**
     * Sets the shutter to the specified position asynchronously.
     * @param position The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutterAsync(double position) throws DeviceException {
        if (position < 0) {
            position = 0;
        }
        if (position > 1) {
            position = 1;
        }
        setAltAsync(position*90.0);
    }    

    /**
     * Sets the altitude the shutter is set to.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract void setAlt(double degrees) throws DeviceException;

    /**
     * Sets the altitude the shutter is set to relative to its current position.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public void setAltRelative(double degreesDelta) throws DeviceException {
        double oldAlt = getAlt();
        double newAlt = oldAlt + degreesDelta;
        if (newAlt < 0) {
            newAlt = 0;
        }
        if (newAlt > 90) {
            newAlt = 90;
        }
        setAlt(newAlt);
    }

    /**
     * Sets the altitude the shutter is set to asynchronously.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract void setAltAsync(double degrees) throws DeviceException;

    /**
     * Sets the altitude the shutter is set to relative to its current position asynchronously.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public void setAltRelativeAsync(double degreesDelta) throws DeviceException {
        double oldAlt = getAlt();
        double newAlt = oldAlt + degreesDelta;
        if (newAlt < 0) {
            newAlt = 0;
        }
        if (newAlt > 90) {
            newAlt = 90;
        }
        setAltAsync(newAlt);
    }

    /**
     * Sets the azimuth the dome is pointing at.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract void slew(double az) throws DeviceException;

    /**
     * Sets the azimuth the dome is pointing at asynchronously.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract void slewAsync(double az) throws DeviceException;

    /**
     * Slew the dome relative to its current position.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public void slewRelative(double degrees) throws DeviceException {
        slew(getAz() + degrees);
    }

    /**
     * Slew the dome relative to its current position asynchronously.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public void slewRelativeAsync(double degrees) throws DeviceException {
        slewAsync(getAz() + degrees);
    }

    /**
     * Parks the dome.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract void park() throws DeviceException;

    /**
     * Parks the dome asynchronously. Note that parking does not involve closing the shutter.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract void parkAsync() throws DeviceException;

    /**
     * Unparks the dome.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract void unpark() throws DeviceException;

    /**
     * Unparks the dome asynchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract void unparkAsync() throws DeviceException;

    /**
     * Sets if the dome is slaved to the telescope.
     * @throws DeviceException If there was an error slaving the dome.
     */
    public abstract void setSlaved(boolean slaved) throws DeviceException;

    /**
     * Sets the dome to the designated home position synchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract void findHome() throws DeviceException;

    /**
     * Sets the dome to the designated home position asynchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract void findHomeAsync() throws DeviceException;

    /**
     * Halts the dome. This should stop the dome from rotating, the shutter from moving, and should
     * disengage slaving.
     * @throws DeviceException If there was an error halting the dome.
     */
    public abstract void halt() throws DeviceException;

    //</editor-fold>
}
