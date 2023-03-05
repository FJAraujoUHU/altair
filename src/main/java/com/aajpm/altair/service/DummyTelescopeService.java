package com.aajpm.altair.service;

import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.TelescopeException;
import com.aajpm.altair.utility.exception.TelescopeUnavailableException;

/**
 * Dummy class to test the logic of the application. Do not use under normal operation.
 * @deprecated This class is only for testing purposes and should not be used in production.
 */
@Deprecated
public class DummyTelescopeService extends TelescopeService {

    boolean isSlaved = false;
    boolean isParked = true;

    double tsAzimuth = 0;
    double tsAltitude = 45;
    double dmAzimuth = 0;

    public DummyTelescopeService() {
        this.state = State.OFF;
        this.currentJob = null;
        this.currentUser = null;
    }

    @Override
    public void start() {
        this.state = State.IDLE;
        isParked = false;
    }

    @Override
    public void start(boolean parked) {
        this.state = State.IDLE;
        isParked = parked;
    }

    @Override
    public void park() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");

        tsAltitude = 45;
        tsAzimuth = 0;
        dmAzimuth = 0;
        isSlaved = false;
        isParked = true;
        this.state = State.IDLE;
    }

    @Override
    public void unpark() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        isParked = false;
    }

    @Override
    public void abortJob() throws TelescopeException {
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new TelescopeException("Telescope is not available");
        
        this.currentJob = null;
    }

    @Override
    public void halt() {
        this.state = State.ERROR;
        this.currentJob = null;
        this.currentUser = null;
    }

    @Override
    public void reset() {
        this.state = State.IDLE;
    }

    @Override
    public boolean takeControl(AltairUser user) {
        if (this.currentUser == null) {
            this.currentUser = user;
            return true;
        } else return false;
    }

    @Override
    public void releaseControl(AltairUser user) throws TelescopeException {
        if (this.currentUser == user) {
            this.currentUser = null;
            this.state = State.IDLE;
        } else throw new TelescopeException("User does not have control of the telescope");
    }

    @Override
    public boolean isSlaved() {
        return isSlaved;
    }

    @Override
    public void setSlaved(boolean slaved) {
        isSlaved = slaved;
        if (slaved)
            dmAzimuth = tsAzimuth;
    }

    @Override
    public boolean connected() {
        return true;
    }

    @Override
    public boolean connected(String device) {
        return true;
    }

    @Override
    public boolean isParked() {
        return isParked;
    }

    @Override
    public boolean isParked(String device) {
        return device.toLowerCase().matches("telescope|dome") ? isParked : false;
    }

    @Override
    public boolean isAtHome() throws TelescopeException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isAtHome'");
    }

    @Override
    public boolean isAtHome(String device) throws TelescopeException {
        if (!device.equalsIgnoreCase("telescope"))
            return tsAltitude == 45 && tsAzimuth == 0;

        if (!device.equalsIgnoreCase("dome"))
            return dmAzimuth == 0;

        throw new TelescopeException("Invalid device name");
    }
    
}
