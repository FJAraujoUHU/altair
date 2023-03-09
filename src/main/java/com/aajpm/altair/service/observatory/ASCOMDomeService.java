package com.aajpm.altair.service.observatory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

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
    public Mono<Double> getAlt() throws DeviceException {
        return this.get("altitude").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getAz() throws DeviceException {
        return this.get("azimuth").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Integer> getShutterStatus() throws DeviceException {
        return this.get("shutterstatus").map(JsonNode::asInt);
    }

    @Override
    public Mono<Boolean> isAtHome() throws DeviceException {
        return this.get("athome").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isParked() throws DeviceException {
        return this.get("atpark").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isShutterOpen() throws DeviceException {
        return this.getShutterStatus().map(status -> status == DomeService.SHUTTER_OPEN);
    }

    @Override
    public Mono<Boolean> isSlaved() throws DeviceException {
        return this.get("slaved").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isSlewing() throws DeviceException {
        return this.get("slewing").map(JsonNode::asBoolean);
    }

    //</editor-fold>
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Setters/Actions">

    @Override
    public void connect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(true));
        this.execute("connected", params);
    }

    @Override
    public void disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(false));
        this.execute("connected", params);
    }

    @Override
    public void closeShutterAwait() throws DeviceException {
        this.put("closeshutter", null).block();
        
    }
    @Override
    public void closeShutter() throws DeviceException {
        this.execute("closeshutter", null);
        
    }

    @Override
    public void findHomeAwait() throws DeviceException {
        this.put("findhome", null).block();
    }

    @Override
    public void findHome() throws DeviceException {
        this.execute("findhome", null);
    }

    @Override
    public void halt() throws DeviceException {
        this.execute("abortslew", null);
    }

    @Override
    public void openShutterAwait() throws DeviceException {
        this.put("openshutter", null).block();
    }

    @Override
    public void openShutter() throws DeviceException {
        this.execute("openshutter", null);
    }

    @Override
    public void parkAwait() throws DeviceException {
        this.put("park", null).block();
    }

    @Override
    public void park() throws DeviceException {
        this.execute("park", null); 
    }

    @Override
    public void setAltAwait(double degrees) throws DeviceException {
        if (degrees < 0) {
            degrees = 0;
        } else if (degrees > 90) {
            degrees = 90;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Altitude", String.valueOf(degrees));
        this.put("slewtoaltitude", params).block();
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
        this.execute("slewtoaltitude", params);
    }

    @Override
    public void setSlaved(boolean slaved) throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Slaved", String.valueOf(slaved));
        this.execute("slaved", params);
    }

    @Override
    public void slewAwait(double az) throws DeviceException {
        az = az % 360;
        if (az < 0) {
            az += 360;
        }
        if (az == 360) {
            az = 0;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Azimuth", String.valueOf(az));
        this.put("slewtoazimuth", params).block();
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
        this.execute("slewtoazimuth", params);  
    }

    @Override
    public void unparkAwait() throws DeviceException {
        // ASCOM does not have an unpark method, so we call findHome instead.
        this.findHomeAwait();
    }

    @Override
    public void unpark() throws DeviceException {
        // ASCOM does not have an unpark method, so we call findHome instead.
        this.findHome(); 
    }

    //</editor-fold>
    ///////////////////////////////// HELPERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Helpers">

    private Mono<JsonNode> get(String action) {
        return client.get("dome", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("dome", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.put("dome", deviceNumber, action, params).subscribe();
    }

    //</editor-fold>
}
