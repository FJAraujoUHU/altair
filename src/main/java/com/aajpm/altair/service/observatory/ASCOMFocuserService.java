package com.aajpm.altair.service.observatory;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

public class ASCOMFocuserService extends FocuserService {

    AlpacaClient client;

    final int deviceNumber;

    Boolean absolute = null;

    public ASCOMFocuserService(AlpacaClient client) {
        this.client = client;
        this.deviceNumber = 0;
    }

    public ASCOMFocuserService(AlpacaClient client, int deviceNumber) {
        this.client = client;
        this.deviceNumber = deviceNumber;
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Integer> getPosition() throws DeviceException {
        return this.get("position").map(JsonNode::asInt);
    }

    @Override
    public Mono<Double> getTemperature() throws DeviceException {
        return this.get("temperature").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Boolean> isTempComp() throws DeviceException {
        return this.get("tempcomp").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isMoving() throws DeviceException {
        return this.get("ismoving").map(JsonNode::asBoolean);
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public void connect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(true));
        this.put("connected", params)
            .doFinally(signalType -> {
                if (signalType == SignalType.ON_COMPLETE) {
                    this.get("absolute")
                        .map(JsonNode::asBoolean)
                        .subscribe(ret -> this.absolute = ret);
                }
                    
            })
            .subscribe();
    }

    @Override
    public void disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(false));
        this.execute("connected", params);
    }

    @Override
    public void move(int position) throws DeviceException {
        if (absolute == null) {
            this.get("absolute")
                .map(JsonNode::asBoolean)
                .subscribe(ret -> this.absolute = ret);
            throw new DeviceException("Focuser not connected/ready");
        }
        int newPosition = Math.max(0, position); // Clamp to 0
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        if (absolute.booleanValue()) {
            params.add("Position", String.valueOf(newPosition));
            this.execute("move", params);
        } else {
            this.getPosition().subscribe(currentPosition -> {
                params.add("Position", String.valueOf(newPosition - currentPosition));
                this.execute("move", params);
            });
        }
    }

    @Override
    public void moveAwait(int position) throws DeviceException {
        if (absolute == null) {
            absolute = this.get("absolute").map(JsonNode::asBoolean).block();
            if (absolute == null)
                throw new DeviceException("Focuser not connected/ready");
        }
        int newPosition = Math.max(0, position); // Clamp to 0
        if (!absolute.booleanValue()) { // If the focuser is relative, we need to convert the position to relative
            int currentPosition = this.getPosition().block();
            newPosition -= currentPosition;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Position", String.valueOf(newPosition));
        this.put("move", params).block();
    }

    @Override
    public void halt() throws DeviceException {
        this.execute("halt", null);
    }

    @Override
    public void setTempComp(boolean enable) throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("TempComp", String.valueOf(enable));
        this.execute("tempcomp", params);
    }
    
    //#endregion
    ///////////////////////////////// HELPERS /////////////////////////////////
    //#region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("focuser", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("focuser", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.put("focuser", deviceNumber, action, params).subscribe();
    }

    //#endregion
}
