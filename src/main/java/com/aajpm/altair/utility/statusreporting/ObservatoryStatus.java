package com.aajpm.altair.utility.statusreporting;

/**
 * DTO for reporting the general status of the observatory
 */
public class ObservatoryStatus {
    // Telescope status
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
    // Dome status
    boolean dmConnected;
    double dmAzimuth;
    int dmShutter;
    boolean dmAtHome;
    boolean dmParked;
    boolean dmSlewing;
    boolean dmSlaved;
    String dmShutterStatus;
    // Focuser status
    boolean fcConnected;
    int fcPosition;
    double fcTemperature;
    boolean fcTempComp;
    boolean fcMoving;
    

    public ObservatoryStatus() {}
    public ObservatoryStatus(TelescopeStatus ts, DomeStatus ds, FocuserStatus fs) {
        // Telescope status
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
        // Dome status
        dmConnected = ds.isConnected();
        dmAzimuth = ds.getAzimuth();
        dmShutter = ds.getShutter();
        dmAtHome = ds.isAtHome();
        dmParked = ds.isParked();
        dmSlewing = ds.isSlewing();
        dmSlaved = ds.isSlaved();
        dmShutterStatus = ds.getShutterStatus();
        // Focuser status
        fcConnected = fs.isConnected();
        fcPosition = fs.getPosition();
        fcTemperature = fs.getTemperature();
        fcTempComp = fs.isTempComp();
        fcMoving = fs.isMoving();
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

    public boolean isFcConnected() {
        return fcConnected;
    }
    public void setFcConnected(boolean fcConnected) {
        this.fcConnected = fcConnected;
    }
    public int getFcPosition() {
        return fcPosition;
    }
    public void setFcPosition(int fcPosition) {
        this.fcPosition = fcPosition;
    }
    public double getFcTemperature() {
        return fcTemperature;
    }
    public void setFcTemperature(double fcTemperature) {
        this.fcTemperature = fcTemperature;
    }
    public boolean isFcTempComp() {
        return fcTempComp;
    }
    public void setFcTempComp(boolean fcTempComp) {
        this.fcTempComp = fcTempComp;
    }
    public boolean isFcMoving() {
        return fcMoving;
    }
    public void setFcMoving(boolean fcMoving) {
        this.fcMoving = fcMoving;
    }

    public static ObservatoryStatus getErrorStatus() {
        return new ObservatoryStatus(TelescopeStatus.getErrorStatus(), DomeStatus.getErrorStatus(), FocuserStatus.getErrorStatus());
    }

}
