package com.aajpm.altair.service.observatory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

// TODO : Migrate from AlpacaObservatoryService to ASCOMTelescopeService
public class ASCOMTelescopeService extends TelescopeService {

    AlpacaClient client;

    final int deviceNumber = 0;

    public ASCOMTelescopeService(AlpacaClient client) {
        this.client = client;
    }

    private JsonNode get(String action) {
        return client.get("telescope", deviceNumber, action);
    }

    private void put(String action, MultiValueMap<String, String> params) {
        client.put("telescope", deviceNumber, action, params);
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'parkAsync'");
    }

    @Override
    public void unpark() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unpark'");
    }

    @Override
    public void findHome() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findHome'");
    }

    @Override
    public void findHomeAsync() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findHomeAsync'");
    }

    @Override
    public void slewToCoords(double ra, double dec) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'slewToCoords'");
    }

    @Override
    public void slewToCoordsAsync(double rightAscension, double declination) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'slewToCoordsAsync'");
    }

    @Override
    public void slewToAltAz(double altitude, double azimuth) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'slewToAltAz'");
    }

    @Override
    public void slewToAltAzAsync(double altitude, double azimuth) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'slewToAltAzAsync'");
    }

    @Override
    public void abortSlew() throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'abortSlew'");
    }

    @Override
    public void setTracking(boolean tracking) throws DeviceException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTracking'");
    }
    
}
