package com.aajpm.altair.utility.statusreporting;

import com.aajpm.altair.service.observatory.TelescopeService.TelescopeStatus;
import com.aajpm.altair.service.observatory.DomeService.DomeStatus;
import com.aajpm.altair.service.observatory.FocuserService.FocuserStatus;

/**
 * DTO for reporting the general status of the observatory
 * @deprecated This class is deprecated and will be replaced by {@link ObservatoryService}'s own implementation
 */
@Deprecated
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
    // Camera status
    boolean caConnected;
    double caTemperature;
    String caCoolerStatus;
    double caCoolerPower;
    String caStatus;
    String caBinning;
    double caStatusCompletion;
    int caSfWidth;
    int caSfHeight;
    int caSfX;
    int caSfY;
    

    public ObservatoryStatus() {}
    public ObservatoryStatus(TelescopeStatus ts, DomeStatus ds, FocuserStatus fs, CameraStatus cs) {
        // Telescope status
        tsConnected = ts.connected();
        tsAltitude = ts.altitude();
        tsAzimuth = ts.azimuth();
        tsRightAscension = ts.rightAscension();
        tsDeclination = ts.declination();
        tsAtHome = ts.atHome();
        tsParked = ts.parked();
        tsSlewing = ts.slewing();
        tsTracking = ts.tracking();
        tsSiderealTime = ts.siderealTime();
        // Dome status
        dmConnected = ds.connected();
        dmAzimuth = ds.azimuth();
        dmShutter = ds.shutter();
        dmAtHome = ds.atHome();
        dmParked = ds.parked();
        dmSlewing = ds.slewing();
        dmSlaved = ds.slaved();
        dmShutterStatus = ds.shutterStatus();
        // Focuser status
        fcConnected = fs.connected();
        fcPosition = fs.position();
        fcTemperature = fs.temperature();
        fcTempComp = fs.tempComp();
        fcMoving = fs.moving();
        // Camera status
        caConnected = cs.isConnected();
        caTemperature = cs.getTemperature();
        caCoolerStatus = cs.getCoolerStatus();
        caCoolerPower = cs.getCoolerPower();
        caStatus = cs.getStatus();
        caBinning = cs.getBinning();
        caStatusCompletion = cs.getStatusCompletion();
        caSfWidth = cs.getSfWidth();
        caSfHeight = cs.getSfHeight();
        caSfX = cs.getSfX();
        caSfY = cs.getSfY();

    }

    public static ObservatoryStatus getErrorStatus() {
        return new ObservatoryStatus(
                    new TelescopeStatus(false, Double.NaN, Double.NaN, Double.NaN, Double.NaN, false, false, false, false, Double.NaN),
                    new DomeStatus(false, Double.NaN, 4, "Error", false, false, false, false),
                    new FocuserStatus(false, -1, Double.NaN, false, false),
                    CameraStatus.getErrorStatus()
                );
    }
    
    /////////////////////////// Getters and Setters ///////////////////////////
    //#region Getters and Setters
    
    // Telescope status
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

    // Dome status
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

    // Focuser status
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
    

    // Camera status
    public boolean isCaConnected() {
        return caConnected;
    }
    public void setCaConnected(boolean caConnected) {
        this.caConnected = caConnected;
    }
    public double getCaTemperature() {
        return caTemperature;
    }
    public void setCaTemperature(double caTemperature) {
        this.caTemperature = caTemperature;
    }
    public String getCaCoolerStatus() {
        return caCoolerStatus;
    }
    public void setCaCoolerStatus(String caCoolerStatus) {
        this.caCoolerStatus = caCoolerStatus;
    }
    public double getCaCoolerPower() {
        return caCoolerPower;
    }
    public void setCaCoolerPower(double caCoolerPower) {
        this.caCoolerPower = caCoolerPower;
    }
    public String getCaStatus() {
        return caStatus;
    }
    public void setCaStatus(String caStatus) {
        this.caStatus = caStatus;
    }
    public String getCaBinning() {
        return caBinning;
    }
    public void setCaBinning(String caBinning) {
        this.caBinning = caBinning;
    }
    public double getCaStatusCompletion() {
        return caStatusCompletion;
    }
    public void setCaStatusCompletion(double caStatusCompletion) {
        this.caStatusCompletion = caStatusCompletion;
    }
    public int getCaSfWidth() {
        return caSfWidth;
    }
    public void setCaSfWidth(int caSfWidth) {
        this.caSfWidth = caSfWidth;
    }
    public int getCaSfHeight() {
        return caSfHeight;
    }
    public void setCaSfHeight(int caSfHeight) {
        this.caSfHeight = caSfHeight;
    }
    public int getCaSfX() {
        return caSfX;
    }
    public void setCaSfX(int caSfX) {
        this.caSfX = caSfX;
    }
    public int getCaSfY() {
        return caSfY;
    }
    public void setCaSfY(int caSfY) {
        this.caSfY = caSfY;
    }

    
    //#endregion

}
