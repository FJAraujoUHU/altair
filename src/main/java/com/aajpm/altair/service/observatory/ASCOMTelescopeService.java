package com.aajpm.altair.service.observatory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

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

    private JsonNode get(String action) {
        return client.get("telescope", deviceNumber, action);
    }

    private void put(String action, MultiValueMap<String, String> params) {
        client.put("telescope", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.execute("telescope", deviceNumber, action, params);
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Getters">

    @Override
    public boolean isConnected() {
        return this.get("connected").asBoolean();
    }

    @Override
    public boolean isParked() throws DeviceException {
        return this.get("atpark").asBoolean();
    }

    @Override
    public boolean isAtHome() throws DeviceException {
        return this.get("athome").asBoolean();
    }

    @Override
    public boolean isSlewing() throws DeviceException {
        return this.get("slewing").asBoolean();
    }

    @Override
    public boolean isTracking() throws DeviceException {
        return this.get("tracking").asBoolean();
    }

    @Override
    public double[] getAltAz() throws DeviceException {
        double altitude = this.get("altitude").asDouble();
        double azimuth = this.get("azimuth").asDouble();
        return new double[] { altitude, azimuth };
    }

    @Override
    public double[] getCoordinates() throws DeviceException {
        double rightAscension = this.get("rightascension").asDouble();
        double declination = this.get("declination").asDouble();
        return new double[] { rightAscension, declination };
    }

    @Override
    public double getSiderealTime() throws DeviceException {
        return this.get("siderealtime").asDouble();
    }

    //</editor-fold>
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //<editor-fold defaultstate="collapsed" desc="Setters/Actions">

    @Override
    public void connect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "true");
        this.put("connected", args);
    }

    @Override
    public void disconnect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "false");
        this.put("connected", args);
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
    public void unpark() throws DeviceException {
        this.put("unpark", null);
    }

    @Override
    public void unparkAsync() throws DeviceException {
        this.execute("unpark", null);
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
    public void slewToCoords(double ra, double dec) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("RightAscension", String.valueOf(ra));
        args.add("Declination", String.valueOf(dec));
        this.put("slewtocoordinates", args);
    }

    @Override
    public void slewToCoordsAsync(double rightAscension, double declination) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("RightAscension", String.valueOf(rightAscension));
        args.add("Declination", String.valueOf(declination));
        this.execute("slewtocoordinates", args);
    }

    @Override
    public void slewToAltAz(double altitude, double azimuth) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("Altitude", String.valueOf(altitude));
        args.add("Azimuth", String.valueOf(azimuth));
        this.put("slewtoaltaz", args);
    }

    @Override
    public void slewToAltAzAsync(double altitude, double azimuth) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("Altitude", String.valueOf(altitude));
        args.add("Azimuth", String.valueOf(azimuth));
        this.execute("slewtoaltaz", args);
    }

    @Override
    public void abortSlew() throws DeviceException {
        this.put("abortslew", null);
    }

    @Override
    public void setTracking(boolean tracking) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Tracking", String.valueOf(tracking));
        this.put("tracking", args);
    }
    
    //</editor-fold>
}
