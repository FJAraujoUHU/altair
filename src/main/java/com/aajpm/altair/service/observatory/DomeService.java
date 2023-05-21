package com.aajpm.altair.service.observatory;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.statusreporting.DomeStatus;

import reactor.core.publisher.Mono;

public abstract class DomeService {

    /////////////////////////////// CONSTANTS /////////////////////////////////
    //#region Constants

    public static final int SHUTTER_OPEN = 0;
    public static final int SHUTTER_CLOSED = 1;
    public static final int SHUTTER_OPENING = 2;
    public static final int SHUTTER_CLOSING = 3;
    public static final int SHUTTER_ERROR = 4;

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

    /**
     * Returns the current status of the dome.
     * @return A POJO containing the current status of the dome.
     * @throws DeviceException If there was an error polling the data.
     */
    public Mono<DomeStatus> getStatus() throws DeviceException {
        Mono<Boolean> connected =   isConnected().onErrorReturn(false);
        Mono<Double> az =           getAz().onErrorReturn(Double.NaN);
        Mono<Double> shutter =      getShutter().onErrorReturn(Double.NaN);
        Mono<String> shutterStatus = getShutterStatus()
            .map(status -> {
                switch (status) {
                    case SHUTTER_OPEN:
                        return "Open";
                    case SHUTTER_CLOSED:
                        return "Closed";
                    case SHUTTER_OPENING:
                        return "Opening";
                    case SHUTTER_CLOSING:
                        return "Closing";
                    default:
                        return "Error";
                }}).onErrorReturn("Error");
        Mono<Boolean> atHome =  isAtHome().onErrorReturn(false);
        Mono<Boolean> parked =  isParked().onErrorReturn(false);
        Mono<Boolean> slaved =  isSlaved().onErrorReturn(false);
        Mono<Boolean> slewing = isSlewing().onErrorReturn(false);

        return Mono.zip(connected, az, shutter, shutterStatus, atHome, parked, slaved, slewing)
                .map(tuple -> {
                    DomeStatus status = new DomeStatus();
                    status.setConnected(tuple.getT1());
                    status.setAzimuth(tuple.getT2());
                    status.setShutter((int) (tuple.getT3()*100));
                    status.setShutterStatus(tuple.getT4());
                    status.setAtHome(tuple.getT5());
                    status.setParked(tuple.getT6());
                    status.setSlaved(tuple.getT7());
                    status.setSlewing(tuple.getT8());
                    return status;
                }).onErrorReturn(DomeStatus.getErrorStatus());
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

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
     * Opens the shutter asynchronously.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract void openShutter() throws DeviceException;

    /**
     * Opens the shutter.
     * @throws DeviceException If there was an error opening the shutter.
     */
    public abstract void openShutterAwait() throws DeviceException;

    /**
     * Closes the shutter asynchronously.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract void closeShutter() throws DeviceException;

    /**
     * Closes the shutter.
     * @throws DeviceException If there was an error closing the shutter.
     */
    public abstract void closeShutterAwait() throws DeviceException;

    /**
     * Sets the shutter to the specified position asynchronously.
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
     * Sets the shutter to the specified position.
     * @param position The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutterAwait(double position) throws DeviceException {
        if (position < 0) {
            position = 0;
        }
        if (position > 1) {
            position = 1;
        }
        setAltAwait(position*90.0);
    }

    /**
     * Sets the shutter to the specified position relative to its current position asynchronously.
     * @param rate The position to set the shutter to, as a fraction of the total range, 0 being closed and 1 being open.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutterRelative(double rate) throws DeviceException {
        getShutter().subscribe(oldShutter -> {
            double newShutter = oldShutter + rate;
            if (newShutter < 0) {
                newShutter = 0;
            }
            if (newShutter > 1) {
                newShutter = 1;
            }

            setShutter(newShutter);
        });
    }

    /**
     * Sets the shutter to the specified position relative to its current position.
     * @param rate The position to set the shutter to, as a fraction of the total range, positive opening the shutter and negative closing it.
     * @throws DeviceException If there was an error setting the shutter.
     */
    public void setShutterRelativeAwait(double rate) throws DeviceException {
        double oldShutter = getShutter().block();
        double newShutter = oldShutter + rate;
        if (newShutter < 0) {
            newShutter = 0;
        }
        if (newShutter > 1) {
            newShutter = 1;
        }

        setShutterAwait(newShutter);
    }

    /**
     * Sets the altitude the shutter is set to asynchronously.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract void setAlt(double degrees) throws DeviceException;
    
    /**
     * Sets the altitude the shutter is set to.
     * @param degrees The altitude the shutter is set to, in degrees, 0 being horizontal.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public abstract void setAltAwait(double degrees) throws DeviceException;

    /**
     * Sets the altitude the shutter is set to relative to its current position asynchronously.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public void setAltRelative(double degreesDelta) throws DeviceException {
        getAlt().subscribe(oldAlt -> {
            double newAlt = oldAlt + degreesDelta;
            if (newAlt < 0) {
                newAlt = 0;
            }
            if (newAlt > 90) {
                newAlt = 90;
            }
            setAlt(newAlt);
        });
    }

    /**
     * Sets the altitude the shutter is set to relative to its current position.
     * @param degreesDelta The amount of degrees to change the altitude of the shutter by. Positive values will open the shutter, negative values will close it.
     * @throws DeviceException If there was an error setting the altitude.
     */
    public void setAltRelativeAwait(double degreesDelta) throws DeviceException {
        double oldAlt = getAlt().block();
        double newAlt = oldAlt + degreesDelta;
        if (newAlt < 0) {
            newAlt = 0;
        }
        if (newAlt > 90) {
            newAlt = 90;
        }
        setAltAwait(newAlt);
    }

    /**
     * Sets the azimuth the dome is pointing at asynchronously.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract void slew(double az) throws DeviceException;

    /**
     * Sets the azimuth the dome is pointing at.
     * @param az The azimuth the dome is pointing at, in degrees, clockwise positive from North.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public abstract void slewAwait(double az) throws DeviceException;

    /**
     * Slew the dome relative to its current position asynchronously.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public void slewRelative(double degrees) throws DeviceException {
        getAz().subscribe(azimuth -> {
            double newAzimuth = azimuth + degrees;
            newAzimuth = newAzimuth % 360; // Wrap around
            slew(newAzimuth);
        });
    }

    /**
     * Slew the dome relative to its current position.
     * @param degrees The number of degrees to slew the dome, positive being clockwise.
     * @throws DeviceException If there was an error setting the azimuth.
     */
    public void slewRelativeAwait(double degrees) throws DeviceException {
        double currentAz = getAz().block();
        double newAzimuth = currentAz + degrees;
        newAzimuth = newAzimuth % 360; // Wrap around
        slewAwait(newAzimuth);
    }

    /**
     * Parks the dome asynchronously. Note that parking does not involve closing the shutter.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract void park() throws DeviceException;

    /**
     * Parks the dome.
     * @throws DeviceException If there was an error parking the dome.
     */
    public abstract void parkAwait() throws DeviceException;

    /**
     * Unparks the dome asynchronously.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract void unpark() throws DeviceException;

    /**
     * Unparks the dome.
     * @throws DeviceException If there was an error unparking the telescope.
     */
    public abstract void unparkAwait() throws DeviceException;

    /**
     * Sets if the dome is slaved to the telescope.
     * @throws DeviceException If there was an error slaving the dome.
     */
    public abstract void setSlaved(boolean slaved) throws DeviceException;

    /**
     * Sets the dome to the designated home position asynchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract void findHome() throws DeviceException;

    /**
     * Sets the dome to the designated home position synchronously.
     * @throws DeviceException If there was an error setting the telescope to the designated home position.
     */
    public abstract void findHomeAwait() throws DeviceException;

    /**
     * Halts the dome. This should stop the dome from rotating, the shutter from moving, and should
     * disengage slaving.
     * @throws DeviceException If there was an error halting the dome.
     */
    public abstract void halt() throws DeviceException;

    //#endregion
}
