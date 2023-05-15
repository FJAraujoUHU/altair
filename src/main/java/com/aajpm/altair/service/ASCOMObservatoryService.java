package com.aajpm.altair.service;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.config.ObservatoryConfig.CameraConfig;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.service.observatory.*;
import com.aajpm.altair.utility.webutils.AlpacaClient;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;

// TODO : Add Job stuff
public class ASCOMObservatoryService extends ObservatoryService {

    AlpacaClient alpaca;

    int connTimeout = 15000;
    int responseTimeout = 60000;

    ASCOMTelescopeService telescope;
    ASCOMDomeService dome;
    ASCOMFocuserService focuser;
    ASCOMCameraService camera;
    ASCOMFilterWheelService filterWheel;

    // TODO : remove the hard coded URL for the camera
    public ASCOMObservatoryService(String baseURL, ObservatoryConfig config) {
        alpaca = new AlpacaClient(baseURL, connTimeout, responseTimeout);
        telescope = new ASCOMTelescopeService(alpaca);
        dome = new ASCOMDomeService(alpaca);
        focuser = new ASCOMFocuserService(alpaca);
        camera = new ASCOMCameraService(new AlpacaClient("http://localhost:11111/", connTimeout, responseTimeout), config.getCamera());
        filterWheel = new ASCOMFilterWheelService(alpaca, config.getFilterWheel());
    }

    public TelescopeService getTelescope() {
        return telescope;
    }

    public DomeService getDome() {
        return dome;
    }

    public FocuserService getFocuser() {
        return focuser;
    }

    public CameraService getCamera() {
        return this.camera;
    }

    public FilterWheelService getFilterWheel() {
        return this.filterWheel;
    }

    @Override
    public void startAwait() throws DeviceUnavailableException {
        telescope.connect();
        dome.connect();

        telescope.unpark();
        telescope.findHome();
        dome.findHomeAwait();

        int tries = 0;
        int waitTime = 1000;

        // Wait for dome and telescope to find home
        while(!(dome.isAtHome().block() && telescope.isAtHome().block()) && tries++ < responseTimeout/waitTime) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                DeviceUnavailableException oops = new DeviceUnavailableException("Interrupted while waiting for dome and telescope to find home");
                oops.addSuppressed(e);
                throw oops; 
            }
            if (tries >= responseTimeout/waitTime) {
                throw new DeviceUnavailableException("Timeout while waiting for dome and telescope to find home");
            }
        }
        this.state = ObservatoryService.State.IDLE;
    }

    @Override
    public void start(boolean parked) throws DeviceUnavailableException {
        if (!parked) {
            startAwait();
        } else {
            telescope.connect();
            dome.connect();

            this.state = ObservatoryService.State.IDLE;
        }
    }

    @Override
    public void start() {
        telescope.connect();
        dome.connect();

        dome.findHome();
        telescope.unpark();
        telescope.findHome();

        this.state = ObservatoryService.State.IDLE;
    }

    @Override
    public void stopAwait() {
        dome.setSlaved(false);
        dome.closeShutter();
        dome.park();
        telescope.park();


        int tries = 0;
        int waitTime = 1000;

        // Wait for dome and telescope to find home
        while(!(dome.isParked().block() && telescope.isParked().block()) && tries++ < responseTimeout/waitTime) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                DeviceUnavailableException oops = new DeviceUnavailableException("Interrupted while waiting for dome and telescope to park");
                oops.addSuppressed(e);
                throw oops; 
            }
            if (tries >= responseTimeout/waitTime) {
                throw new DeviceUnavailableException("Timeout while waiting for dome and telescope to park");
            }
        }

        telescope.disconnect();
        dome.disconnect();
        this.state = ObservatoryService.State.OFF;
    }

    @Override
    public void abortJob() throws DeviceException {
        // TODO Auto-generated method stub
        if (this.state == State.OFF || this.state == State.ERROR)
            throw new DeviceException("Telescope is not available");
        
        this.currentJob = null;
        throw new UnsupportedOperationException("Unimplemented method 'abortJob'");
    }


    @Override
    public void halt() {
        // TODO Auto-generated method stub
        this.state = State.ERROR;
        this.currentJob = null;
        this.currentUser = null;
        throw new UnsupportedOperationException("Unimplemented method 'halt'");
    }


    @Override
    public void reset() throws DeviceException {
        this.state = ObservatoryService.State.IDLE;
    }


    @Override
    public boolean takeControl(AltairUser user) throws DeviceException {
        if (this.state != ObservatoryService.State.IDLE) {
            return false;
        }
        if (this.currentUser != null) {
            return false;
        }
        this.currentUser = user;
        alpaca.setCurrentUser(user);

        this.state = (user == null) ? ObservatoryService.State.IDLE : ObservatoryService.State.MANUAL;
        return true;
    }

    @Override
    public boolean takeControl(AltairUser user, boolean force) throws DeviceException {
        if (!force) {
            return takeControl(user);
        }
        this.currentUser = user;
        alpaca.setCurrentUser(user);
        
        this.state = (user == null) ? ObservatoryService.State.IDLE : ObservatoryService.State.MANUAL;
        return true;
    }

    @Override
    public void releaseControl() {
        this.currentUser = null;
        alpaca.setCurrentUser(null);
        this.state = ObservatoryService.State.IDLE;
    }

    @Override
    public void releaseControl(AltairUser user) throws DeviceException {
        if (this.currentUser != user) {
            throw new DeviceException("User does not have control of observatory");
        }
        releaseControl();
    }

}