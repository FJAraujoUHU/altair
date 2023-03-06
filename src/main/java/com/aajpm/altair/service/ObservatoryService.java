package com.aajpm.altair.service;

import com.aajpm.altair.entity.Job;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.*;


// TODO : Add other useful methods to this class
// slewing, syncing, shutter control, tracking, abort slewing, focuser, camera.
public abstract class ObservatoryService {

    public enum State {
        OFF,        // Telescope is off
        IDLE,       // Telescope is unparked, but not doing anything
        AUTO,       // Telescope is performing a job from the queue
        MANUAL,     // Telescope is being used manually by an advanced user
        BUSY,       // Telescope is slewing or performing a job that can't be interrupted
        ERROR       // Telescope has encountered an error, must be reset by an admin
    }

    protected State state;

    protected Job currentJob;

    protected AltairUser currentUser;

    /**
     * Get the current state of the telescope
     * @return The current state of the telescope as a string.
     */
    public String getState() { return state.name(); }

    /**
     * Returns true if the server is connected to the specified device.
     * @param device The device to check.
     * @return True if the server is connected to the specified device, false otherwise.
     */
    public abstract boolean connected(String device);
    
    /**
     * Starts the observatory and sets it up ready for use.
     * @throws DeviceUnavailableException If the telescope is unaccessible.
    */
    public abstract void start() throws DeviceUnavailableException;

    /**
     * Starts the observatory, and optionally unparks the telescope and dome.
     * @param parked True if the devices should be left parked, false otherwise.
     * @throws DeviceUnavailableException If the telescope is unaccessible.
     */
    public abstract void start(boolean parked) throws DeviceUnavailableException;

    /**
     * Checks if the specified device is parked.
     * @return True if the device is parked, false otherwise.
     * @throws DeviceException If there was an error getting the parked status or the device can't be checked.
     */
    public abstract boolean isParked(String device) throws DeviceException;

    /**
     * Checks if the specified device is at the designated home position.
     * @param device The device to check.
     * @return True if the device is at the designated home position, false otherwise.
     * @throws DeviceException If there was an error getting the AtHome status or the device can't be checked.
     */
    public abstract boolean isAtHome(String device) throws DeviceException;

    /**
     * Returns true if the telescope and the dome are slaved together.
     * @return True if the telescope and the dome are slaved together, false otherwise.
     * @throws DeviceException If there was an error getting the slaved status.
     */
    // TODO : This should be moved to the dome service
    public abstract boolean isSlaved() throws DeviceException;

    /**
     * Slaves/unslaves the telescope and the dome together.
     * @throws DeviceException If there was an error slaving the telescope and the dome.
     */
    // TODO : This should be moved to the dome service
    public abstract void setSlaved(boolean slaved) throws DeviceException;

    /**
     * Returns the current job being performed by the telescope, if any.
     * @return The current job being performed by the telescope, or null if there is none.
     */
    public Job getCurrentJob() { return currentJob; }

    /**
     * Stops the current job and returns to the idle state.
     * The job should be stopped gracefully and not discarded, and the telescope left in a safe state.
     * @throws DeviceException If there was an error stopping the job.
     */
    public abstract void abortJob() throws DeviceException;

    /**
     * Stops whatever the telescope is doing and goes into an error state.
     */
    public abstract void halt();

    /**
     * Resets the telescope to a state where it can be used again.
     * This may require a manual reset of the telescope, and so this could be called
     * by an admin after manually fixing the issue.
     * @throws DeviceException If the telescope can't be safely used again.
     */
    public abstract void reset() throws DeviceException;

    /**
     * Gets the current user of the telescope.
     * @return The current user of the telescope if it is in manual mode, or null if there is none/it is not in manual mode.
     */
    public AltairUser getCurrentUser() { return (state == State.MANUAL) ? currentUser : null; }

    /**
     * Takes manual control of the telescope for the given user.
     * If the telescope is not idle or being in use by other users, this will fail.
     * @param user The user to get control for.
     * @return True if the user got control, false if the telescope is already in use.
     * @throws DeviceException If there was an error getting control.
     */
    public abstract boolean takeControl(AltairUser user) throws DeviceException;

    /**
     * Releases manual control of the telescope for the given user.
     * @param user The user to release control for.
     * @throws DeviceException If the user does not have control, or if there was an error releasing control.
     */
    public abstract void releaseControl(AltairUser user) throws DeviceException;

    

}
