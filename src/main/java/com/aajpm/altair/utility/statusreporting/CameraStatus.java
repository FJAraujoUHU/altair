package com.aajpm.altair.utility.statusreporting;

import com.aajpm.altair.service.observatory.CameraService;

/**
 * DTO for reporting the status of a camera
 */
// TODO: Add more fields as needed
public class CameraStatus {
    boolean connected;
    double temperature;
    String coolerStatus;
    double coolerPower;
    String status;
    String binning;
    double statusCompletion;
    int sfWidth;
    int sfHeight;
    int sfX;
    int sfY;

    public int getSfWidth() {
        return sfWidth;
    }
    public void setSfWidth(int sfWidth) {
        this.sfWidth = sfWidth;
    }
    public int getSfHeight() {
        return sfHeight;
    }
    public void setSfHeight(int sfHeight) {
        this.sfHeight = sfHeight;
    }
    public int getSfX() {
        return sfX;
    }
    public void setSfX(int sfX) {
        this.sfX = sfX;
    }
    public int getSfY() {
        return sfY;
    }
    public void setSfY(int sfY) {
        this.sfY = sfY;
    }
    public void setSubframe(int x, int y, int width, int height) {
        this.sfX = x;
        this.sfY = y;
        this.sfWidth = width;
        this.sfHeight = height;
    }
    public boolean isConnected() {
        return connected;
    }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public double getTemperature() {
        return temperature;
    }
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    public String getCoolerStatus() {
        return coolerStatus;
    }
    public void setCoolerStatus(String coolerStatus) {
        this.coolerStatus = coolerStatus;
    }
    public void setCoolerStatus(int status) {
        switch(status) {
            case CameraService.COOLER_OFF:
                this.coolerStatus = "Off";
                return;
            case CameraService.COOLER_COOLDOWN:
                this.coolerStatus = "Cooling down";
                return;
            case CameraService.COOLER_WARMUP:
                this.coolerStatus = "Warming up";
                return;
            case CameraService.COOLER_STABLE:
                this.coolerStatus = "Stable";
                return;
            case CameraService.COOLER_SATURATED:
                this.coolerStatus = "Saturated";
                return;
            case CameraService.COOLER_ACTIVE:
                this.coolerStatus = "Active";
                return;
            case CameraService.COOLER_ERROR:
                this.coolerStatus = "Error";
                return;
            default:
                this.coolerStatus = "Unknown";
                return;
        }
    }
    public double getCoolerPower() {
        return coolerPower;
    }
    public void setCoolerPower(double coolerPower) {
        this.coolerPower = coolerPower;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public void setStatus(int status) {
        switch(status) {
            case CameraService.STATUS_IDLE:
                this.status = "Idle";
                return;
            case CameraService.STATUS_WAITING:
                this.status = "Waiting";
                return;
            case CameraService.STATUS_EXPOSING:
                this.status = "Exposing";
                return;
            case CameraService.STATUS_READING:
                this.status = "Reading";
                return;
            case CameraService.STATUS_DOWNLOADING:
                this.status = "Downloading";
                return;
            case CameraService.STATUS_ERROR:
                this.status = "Error";
                return;
            default:
                this.status = "Unknown";
                return;
        }
    }
    public String getBinning() {
        return binning;
    }
    public void setBinning(String binning) {
        this.binning = binning;
    }
    public void setBinning(int bin) {
        this.binning = "" + bin + "x" + bin;
    }
    public void setBinning(int binx, int biny) {
        this.binning = "" + binx + "x" + biny;
    }
    public double getStatusCompletion() {
        return statusCompletion;
    }
    public void setStatusCompletion(double statusCompletion) {
        this.statusCompletion = statusCompletion;
    }

    public static CameraStatus getErrorStatus() {
        CameraStatus status = new CameraStatus();
        status.setConnected(false);
        status.setCoolerStatus("Error");
        status.setCoolerPower(Double.NaN);
        status.setStatus("Error");
        status.setStatusCompletion(Double.NaN);
        status.setTemperature(Double.NaN);
        status.setBinning("Error");
        return status;
    }
    
}
