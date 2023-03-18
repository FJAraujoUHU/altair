package com.aajpm.altair.utility.statusreporting;

/**
 * POJO for reporting the status of a focuser
 */
public class FocuserStatus {
    boolean connected;
    int position;
    double temperature;
    boolean tempComp;
    boolean moving;

    public boolean isConnected() {
        return connected;
    }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public int getPosition() {
        return position;
    }
    public void setPosition(int position) {
        this.position = position;
    }
    public double getTemperature() {
        return temperature;
    }
    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
    public boolean isTempComp() {
        return tempComp;
    }
    public void setTempComp(boolean tempComp) {
        this.tempComp = tempComp;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public static FocuserStatus getErrorStatus() {
        FocuserStatus status = new FocuserStatus();
        status.setConnected(false);
        status.setPosition(-1);
        status.setTemperature(0);
        status.setTempComp(false);
        status.setMoving(false);
        return status;
    }
}
