package com.aajpm.altair.utility.statusreporting;

import com.aajpm.altair.service.observatory.CameraService;

/**
 * DTO for reporting the status of a camera
 */
// TODO: Add more fields as needed
public class CameraStatus {
    boolean connected;
    double temperature;
    boolean coolerOn;
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
    public boolean isCoolerOn() {
        return coolerOn;
    }
    public void setCoolerOn(boolean coolerOn) {
        this.coolerOn = coolerOn;
    }
    public void setStatusCompletion(double statusCompletion) {
        this.statusCompletion = statusCompletion;
    }
    
}
