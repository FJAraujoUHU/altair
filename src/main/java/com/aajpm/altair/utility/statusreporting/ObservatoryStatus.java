package com.aajpm.altair.utility.statusreporting;

/**
 * POJO for reporting the general status of the observatory
 */
public class ObservatoryStatus {
    boolean tsConnected;
    double tsAltitude;
    double tsAzimuth;
    double tsRightAscension;
    double tsDeclination;
    boolean tsAtHome;
    boolean tsParked;
    boolean tsSlewing;
    boolean tsTracking;
    double tsSiderealTime;
    boolean dmConnected;
    double dmAzimuth;
    int dmShutter;
    boolean dmAtHome;
    boolean dmParked;
    boolean dmSlewing;
    boolean dmSlaved;
    String dmShutterStatus;

    public ObservatoryStatus() {}
    public ObservatoryStatus(TelescopeStatus ts, DomeStatus ds) {
        tsConnected = ts.isConnected();
        tsAltitude = ts.getAltitude();
        tsAzimuth = ts.getAzimuth();
        tsRightAscension = ts.getRightAscension();
        tsDeclination = ts.getDeclination();
        tsAtHome = ts.isAtHome();
        tsParked = ts.isParked();
        tsSlewing = ts.isSlewing();
        tsTracking = ts.isTracking();
        tsSiderealTime = ts.getSiderealTime();
        dmConnected = ds.isConnected();
        dmAzimuth = ds.getAzimuth();
        dmShutter = ds.getShutter();
        dmAtHome = ds.isAtHome();
        dmParked = ds.isParked();
        dmSlewing = ds.isSlewing();
        dmSlaved = ds.isSlaved();
        dmShutterStatus = ds.getShutterStatus();
    }
    public boolean isTsConnected() {
        return tsConnected;
    }
    public void setTsConnected(boolean tsConnected) {
        this.tsConnected = tsConnected;
    }
    public double getTsAltitude() {
        return tsAltitude;
    }
    public void setTsAltitude(double tsAltitude) {
        this.tsAltitude = tsAltitude;
    }
    public double getTsAzimuth() {
        return tsAzimuth;
    }
    public void setTsAzimuth(double tsAzimuth) {
        this.tsAzimuth = tsAzimuth;
    }
    public double getTsRightAscension() {
        return tsRightAscension;
    }
    public void setTsRightAscension(double tsRightAscension) {
        this.tsRightAscension = tsRightAscension;
    }
    public double getTsDeclination() {
        return tsDeclination;
    }
    public void setTsDeclination(double tsDeclination) {
        this.tsDeclination = tsDeclination;
    }
    public boolean isTsAtHome() {
        return tsAtHome;
    }
    public void setTsAtHome(boolean tsAtHome) {
        this.tsAtHome = tsAtHome;
    }
    public boolean isTsParked() {
        return tsParked;
    }
    public void setTsParked(boolean tsParked) {
        this.tsParked = tsParked;
    }
    public boolean isTsSlewing() {
        return tsSlewing;
    }
    public void setTsSlewing(boolean tsSlewing) {
        this.tsSlewing = tsSlewing;
    }
    public boolean isTsTracking() {
        return tsTracking;
    }
    public void setTsTracking(boolean tsTracking) {
        this.tsTracking = tsTracking;
    }
    public double getTsSiderealTime() {
        return tsSiderealTime;
    }
    public void setTsSiderealTime(double tsSiderealTime) {
        this.tsSiderealTime = tsSiderealTime;
    }
    public boolean isDmConnected() {
        return dmConnected;
    }
    public void setDmConnected(boolean dmConnected) {
        this.dmConnected = dmConnected;
    }
    public double getDmAzimuth() {
        return dmAzimuth;
    }
    public void setDmAzimuth(double dmAzimuth) {
        this.dmAzimuth = dmAzimuth;
    }
    public int getDmShutter() {
        return dmShutter;
    }
    public void setDmShutter(int dmShutter) {
        this.dmShutter = dmShutter;
    }
    public boolean isDmAtHome() {
        return dmAtHome;
    }
    public void setDmAtHome(boolean dmAtHome) {
        this.dmAtHome = dmAtHome;
    }
    public boolean isDmParked() {
        return dmParked;
    }
    public void setDmParked(boolean dmParked) {
        this.dmParked = dmParked;
    }
    public boolean isDmSlewing() {
        return dmSlewing;
    }
    public void setDmSlewing(boolean dmSlewing) {
        this.dmSlewing = dmSlewing;
    }
    public boolean isDmSlaved() {
        return dmSlaved;
    }
    public void setDmSlaved(boolean dmSlaved) {
        this.dmSlaved = dmSlaved;
    }
    public String getDmShutterStatus() {
        return dmShutterStatus;
    }
    public void setDmShutterStatus(String dmShutterStatus) {
        this.dmShutterStatus = dmShutterStatus;
    }

}
