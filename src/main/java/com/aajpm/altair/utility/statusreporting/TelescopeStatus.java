package com.aajpm.altair.utility.statusreporting;

/**
 * POJO for reporting telescope status
 */
public class TelescopeStatus {
    boolean connected;
    double altitude;
    double azimuth;
    double rightAscension;
    double declination;
    boolean atHome;
    boolean parked;
    boolean slewing;
    boolean tracking;
    double siderealTime;

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

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getRightAscension() {
        return rightAscension;
    }

    public void setRightAscension(double rightAscension) {
        this.rightAscension = rightAscension;
    }

    public double getDeclination() {
        return declination;
    }

    public void setDeclination(double declination) {
        this.declination = declination;
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

    public double getSiderealTime() {
        return siderealTime;
    }

    public void setSiderealTime(double siderealTime) {
        this.siderealTime = siderealTime;
    }

    public boolean isTracking() {
        return tracking;
    }

    public void setTracking(boolean tracking) {
        this.tracking = tracking;
    }

    public static TelescopeStatus getErrorStatus() {
        TelescopeStatus status = new TelescopeStatus();
        status.setConnected(false);
        status.setAltitude(0.0);
        status.setAzimuth(0.0);
        status.setRightAscension(0.0);
        status.setDeclination(0.0);
        status.setAtHome(false);
        status.setParked(false);
        status.setSlewing(false);
        status.setTracking(false);
        status.setSiderealTime(0.0);
        return status;
    }
}