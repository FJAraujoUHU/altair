package com.aajpm.altair.service.observatory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

public class ASCOMDomeService extends DomeService {
    
    AlpacaClient client;

    final int deviceNumber;

    public ASCOMDomeService(AlpacaClient client) {
        this.client = client;
        this.deviceNumber = 0;
    }

    public ASCOMDomeService(AlpacaClient client, int deviceNumber) {
        this.client = client;
        this.deviceNumber = deviceNumber;
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Getters">
    
    @Override
    public double getAlt() throws DeviceException {
        return this.get("altitude").asDouble();
    }

    @Override
    public double getAz() throws DeviceException {
        return this.get("azimuth").asDouble();
    }

    @Override
    public int getShutterStatus() throws DeviceException {
        return this.get("shutterstatus").asInt();
    }

    @Override
    public boolean isAtHome() throws DeviceException {
        return this.get("athome").asBoolean();
    }

    @Override
    public boolean isConnected() {
        return this.get("connected").asBoolean();
    }

    @Override
    public boolean isParked() throws DeviceException {
        return this.get("atpark").asBoolean();
    }

    @Override
    public boolean isShutterOpen() throws DeviceException {
        return this.getShutterStatus() == 0;
    }

    @Override
    public boolean isSlaved() throws DeviceException {
        return this.get("slaved").asBoolean();
    }

    @Override
    public boolean isSlewing() throws DeviceException {
        return this.get("slewing").asBoolean();
    }

    //</editor-fold>
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Setters/Actions">

    @Override
    public void connect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(true));
        this.put("connected", params);
    }

    @Override
    public void disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(false));
        this.put("connected", params);
    }

    @Override
    public void closeShutter() throws DeviceException {
        this.put("closeshutter", null);
        
    }
    @Override
    public void closeShutterAsync() throws DeviceException {
        this.execute("closeshutter", null);
        
    }

    @Override
    public void findHome() throws DeviceException {
        this.put("findhome", null);
    }

    @Override
    public void findHomeAsync() throws DeviceException {
        this.execute("findhome", null);
    }

    @Override
    public void halt() throws DeviceException {
        this.put("abortslew", null);
    }

    @Override
    public void openShutter() throws DeviceException {
        this.put("openshutter", null);
    }

    @Override
    public void openShutterAsync() throws DeviceException {
        this.execute("openshutter", null);
    }

    @Override
    public void park() throws DeviceException {
        this.put("park", null);
    }

    @Override
    public void parkAsync() throws DeviceException {
        this.execute("park", null); 
    }

    @Override
    public void setAlt(double degrees) throws DeviceException {
        if (degrees < 0) {
            degrees = 0;
        } else if (degrees > 90) {
            degrees = 90;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Altitude", String.valueOf(degrees));
        this.put("slewtoaltitude", params);
    }

    @Override
    public void setAltAsync(double degrees) throws DeviceException {
        if (degrees < 0) {
            degrees = 0;
        } else if (degrees > 90) {
            degrees = 90;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Altitude", String.valueOf(degrees));
        this.execute("slewtoaltitude", params);
    }

    @Override
    public void setSlaved(boolean slaved) throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Slaved", String.valueOf(slaved));
        this.put("slaved", params);
    }

    @Override
    public void slew(double az) throws DeviceException {
        az = az % 360;
        if (az < 0) {
            az += 360;
        }
        if (az == 360) {
            az = 0;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Azimuth", String.valueOf(az));
        this.put("slewtoazimuth", params);
    }

    @Override
    public void slewAsync(double az) throws DeviceException {
        az = az % 360;
        if (az < 0) {
            az += 360;
        }
        if (az == 360) {
            az = 0;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Azimuth", String.valueOf(az));
        this.execute("slewtoazimuth", params);  
    }

    @Override
    public void unpark() throws DeviceException {
        // ASCOM does not have an unpark method, so we call findHome instead.
        this.findHome();
    }

    @Override
    public void unparkAsync() throws DeviceException {
        // ASCOM does not have an unpark method, so we call findHome instead.
        this.findHomeAsync(); 
    }

    //</editor-fold>
    ///////////////////////////////// HELPERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Helpers">

    private JsonNode get(String action) {
        return client.get("dome", deviceNumber, action);
    }

    private void put(String action, MultiValueMap<String, String> params) {
        client.put("dome", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.execute("dome", deviceNumber, action, params);
    }

    //</editor-fold>
}
