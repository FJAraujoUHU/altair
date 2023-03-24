package com.aajpm.altair.utility.statusreporting;

/**
 * DTO for reporting the status of a camera
 */
// TODO: Add more fields as needed
public class CameraStatus {
    boolean connected;
    double temperature;
    double coolerPower;
    String status;
    double statusCompletion;

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
}
