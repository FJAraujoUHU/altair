package com.aajpm.altair.service;

import com.aajpm.altair.entity.Job;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.*;


// TODO : Add other useful methods to this class
// slewing, syncing, shutter control, tracking, abort slewing, focuser, camera.
public abstract class TelescopeService {

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
     * Returns true if the telescope is connected.
     * @return True if the telescope is connected, false otherwise.
     */
    public abstract boolean connected();

    /**
     * Returns true if the server is connected to the specified device.
     * @param device The device to check.
     * @return True if the server is connected to the specified device, false otherwise.
     */
    public abstract boolean connected(String device);
    
    /**
     * Starts the telescope and sets it up ready for use.
     * @throws TelescopeUnavailableException If the telescope is unaccessible.
    */
    public abstract void start() throws TelescopeUnavailableException;

    /**
     * Starts the telescope, and optionally unparks it.
     * @param parked True if the telescope should be left parked, false otherwise.
     * @throws TelescopeUnavailableException If the telescope is unaccessible.
     */
    public abstract void start(boolean parked) throws TelescopeUnavailableException;

    /**
     * Parks the telescope.
     * @throws TelescopeException If the telescope is in use, or if there is an error.
     */
    public abstract void park() throws TelescopeException;

    /**
     * Unparks the telescope. If it was already unparked, this does nothing.
     * @throws TelescopeException If the telescope is in use, or if there is an error.
     */
    public abstract void unpark() throws TelescopeException;

    /**
     * Returns true if the telescope is parked.
     * @return True if the telescope is parked, false otherwise.
     * @throws TelescopeException If there was an error getting the parked status.
     */
    public abstract boolean isParked() throws TelescopeException;

    /**
     * Checks if the specified device is parked.
     * @return True if the device is parked, false otherwise.
     * @throws TelescopeException If there was an error getting the parked status or the device can't be checked.
     */
    public abstract boolean isParked(String device) throws TelescopeException;

    /**
     * Returns true if the telescope is at the designated home position.
     * @return True if the telescope is at the designated home position, false otherwise.
     * @throws TelescopeException If there was an error getting the AtHome status.
     */
    public abstract boolean isAtHome() throws TelescopeException;

    /**
     * Checks if the specified device is at the designated home position.
     * @param device The device to check.
     * @return True if the device is at the designated home position, false otherwise.
     * @throws TelescopeException If there was an error getting the AtHome status or the device can't be checked.
     */
    public abstract boolean isAtHome(String device) throws TelescopeException;

    /**
     * Returns true if the telescope and the dome are slaved together.
     * @return True if the telescope and the dome are slaved together, false otherwise.
     * @throws TelescopeException If there was an error getting the slaved status.
     */
    public abstract boolean isSlaved() throws TelescopeException;

    /**
     * Slaves/unslaves the telescope and the dome together.
     * @throws TelescopeException If there was an error slaving the telescope and the dome.
     */
    public abstract void setSlaved(boolean slaved) throws TelescopeException;

    /**
     * Returns the current job being performed by the telescope, if any.
     * @return The current job being performed by the telescope, or null if there is none.
     */
    public Job getCurrentJob() { return currentJob; }

    /**
     * Stops the current job and returns to the idle state.
     * The job should be stopped gracefully and not discarded, and the telescope left in a safe state.
     * @throws TelescopeException If there was an error stopping the job.
     */
    public abstract void abortJob() throws TelescopeException;

    /**
     * Stops whatever the telescope is doing and goes into an error state.
     */
    public abstract void halt();

    /**
     * Resets the telescope to a state where it can be used again.
     * This may require a manual reset of the telescope, and so this could be called
     * by an admin after manually fixing the issue.
     * @throws TelescopeException If the telescope can't be safely used again.
     */
    public abstract void reset() throws TelescopeException;

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
     * @throws TelescopeException If there was an error getting control.
     */
    public abstract boolean takeControl(AltairUser user) throws TelescopeException;

    /**
     * Releases manual control of the telescope for the given user.
     * @param user The user to release control for.
     * @throws TelescopeException If the user does not have control, or if there was an error releasing control.
     */
    public abstract void releaseControl(AltairUser user) throws TelescopeException;

}
