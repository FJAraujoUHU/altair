package com.aajpm.altair.utility.statusreporting;

/**
 * POJO for reporting the status of a dome
 */
public class DomeStatus {
    boolean connected;
    double azimuth;
    int shutter;
    boolean atHome;
    boolean parked;
    boolean slewing;
    boolean slaved;
    String shutterStatus;

    
    public boolean isConnected() {
        return connected;
    }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public double getAzimuth() {
        return azimuth;
    }
    public void setAzimuth(double azimuth) {
        this.azimuth = azimuth;
    }
    public double getShutter() {
        return shutter;
    }
    public void setShutter(int porcentage) {
        this.shutter = porcentage;
    }
    public boolean isAtHome() {
        return atHome;
    }
    public void setAtHome(boolean atHome) {
        this.atHome = atHome;
    }
    public boolean isParked() {
        return parked;
    }
    public void setParked(boolean parked) {
        this.parked = parked;
    }
    public boolean isSlewing() {
        return slewing;
    }
    public void setSlewing(boolean slewing) {
        this.slewing = slewing;
    }
    public boolean isSlaved() {
        return slaved;
    }
    public void setSlaved(boolean slaved) {
        this.slaved = slaved;
    }
    public String getShutterStatus() {
        return shutterStatus;
    }
    public void setShutterStatus(String shutterStatus) {
        this.shutterStatus = shutterStatus;
    }

    
}
