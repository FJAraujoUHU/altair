package com.aajpm.altair.service.observatory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClientException;

import com.aajpm.altair.utility.exception.ASCOMException;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

public class ASCOMTelescopeService extends TelescopeService {

    AlpacaClient client;

    final int deviceNumber;

    public ASCOMTelescopeService(AlpacaClient client) {
        this.client = client;
        this.deviceNumber = 0;
    }

    public ASCOMTelescopeService(AlpacaClient client, int deviceNumber) {
        this.client = client;
        this.deviceNumber = deviceNumber;
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Getters">

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isParked() throws DeviceException {
        return this.get("atpark").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isAtHome() throws DeviceException {
        return this.get("athome").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isSlewing() throws DeviceException {
        return this.get("slewing").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isTracking() throws DeviceException {
        return this.get("tracking").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<double[]> getAltAz() throws DeviceException {
        return this
            .get("altitude").map(JsonNode::asDouble)
            .zipWith(this.get("azimuth").map(JsonNode::asDouble))
            .map(tuple -> new double[] { tuple.getT1(), tuple.getT2() });

    }

    @Override
    public Mono<double[]> getCoordinates() throws DeviceException {
        return this
            .get("rightascension").map(JsonNode::asDouble)
            .zipWith(this.get("declination").map(JsonNode::asDouble))
            .map(tuple -> new double[] { tuple.getT1(), tuple.getT2() });
    }

    @Override
    public Mono<Double> getSiderealTime() throws DeviceException {
        return this.get("siderealtime").map(JsonNode::asDouble);
    }

    //</editor-fold>
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Setters/Actions">

    @Override
    public void connect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "true");
        this.execute("connected", args);
    }

    @Override
    public void disconnect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "false");
        this.execute("connected", args);
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
    public void unparkAwait() throws DeviceException {
        this.put("unpark", null).block();
    }

    @Override
    public void unpark() throws DeviceException {
        this.execute("unpark", null);
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
    public void slewToCoordsAwait(double ra, double dec) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("RightAscension", String.valueOf(ra));
        args.add("Declination", String.valueOf(dec));
        this.put("slewtocoordinates", args).block();
    }

    @Override
    public void slewToCoords(double rightAscension, double declination) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("RightAscension", String.valueOf(rightAscension));
        args.add("Declination", String.valueOf(declination));
        this.execute("slewtocoordinates", args);
    }

    @Override
    public void slewToAltAzAwait(double altitude, double azimuth) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("Altitude", String.valueOf(altitude));
        args.add("Azimuth", String.valueOf(azimuth));
        this.put("slewtoaltaz", args).block();
    }

    @Override
    public void slewToAltAz(double altitude, double azimuth) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("Altitude", String.valueOf(altitude));
        args.add("Azimuth", String.valueOf(azimuth));
        this.execute("slewtoaltaz", args);
    }

    @Override
    public void abortSlew() throws DeviceException {
        this.execute("abortslew", null);
    }

    @Override
    public void setTracking(boolean tracking) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Tracking", String.valueOf(tracking));
        this.execute("tracking", args);
    }
    
    //</editor-fold>
    ///////////////////////////////// HELPERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Helpers">

    private Mono<JsonNode> get(String action) {
        return client.get("telescope", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("telescope", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) throws DeviceUnavailableException, ASCOMException, WebClientException {
        client.put("telescope", deviceNumber, action, params).subscribe();
    }

    //</editor-fold>
}
