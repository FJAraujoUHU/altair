package com.aajpm.altair.service;

import com.aajpm.altair.entity.Job;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.*;


// TODO : Add other useful methods to this class
public abstract class TelescopeService {

    public enum State {
        OFF,        // Telescope is off
        PARKED,     // Telescope is parked
        IDLE,       // Telescope is unparked, but not doing anything
        AUTO,       // Telescope is performing a job from the queue
        MANUAL,     // Telescope is being used manually by an advanced user
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
