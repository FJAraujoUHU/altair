package com.aajpm.altair.service.observatory;

import java.time.Duration;
import java.util.Objects;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ASCOMFocuserService extends FocuserService {

    AlpacaClient client;

    final int deviceNumber;

    final int statusUpdateInterval; // how often checks the state of the device for synchronous methods

    final long synchronousTimeout; // how long to wait for synchronous methods to complete

    private FocuserCapabilities capabilities;


    public ASCOMFocuserService(AlpacaClient client, int statusUpdateInterval, long synchronousTimeout) {
        this(client, 0 , statusUpdateInterval, synchronousTimeout);
    }

    public ASCOMFocuserService(AlpacaClient client, int deviceNumber, int statusUpdateInterval, long synchronousTimeout) {
        this.client = client;
        this.deviceNumber = deviceNumber;
        this.statusUpdateInterval = statusUpdateInterval;
        this.synchronousTimeout = synchronousTimeout;
        this.getCapabilities().onErrorComplete().subscribe(); // attempt to get the device's capabilities
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
        return getCapabilities().flatMap(caps -> {
            if (caps.canTempComp()) {
                return this.get("temperature").map(JsonNode::asDouble);
            } else {
                return Mono.error(new DeviceException("Device does not support temperature compensation"));
            }
        });
    }

    @Override
    public Mono<Boolean> isTempComp() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canTempComp()) {
                return this.get("tempcomp").map(JsonNode::asBoolean);
            } else {
                return Mono.error(new DeviceException("Device does not support temperature compensation"));
            }
        });
    }

    @Override
    public Mono<Boolean> isMoving() throws DeviceException {
        return this.get("ismoving").map(JsonNode::asBoolean);
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public Mono<Void> connect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", String.valueOf(true));
        return this.put("connected", args)
            .doOnSuccess(v -> this.getCapabilities().subscribe())   // attempt to get the device's capabilities
            .then();
    }

    @Override
    public Mono<Void> disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(false));
        return this.put("connected", params).then();
    }

    @Override
    public Mono<Void> move(int position) throws DeviceException {
        return this.getCapabilities().zipWith(this.getPosition()).flatMap(tuple -> {
            int maxIncrement = tuple.getT1().maxIncrement();
            int maxStep = tuple.getT1().maxStep();
            int startPos = tuple.getT2();
            int targetPos = Math.min(Math.max(0, position), maxStep); // Clamp to limits
            
            // Calculate the number of moves required to reach the target position
            int delta = targetPos - startPos;
            int moves = (int) Math.ceil(Math.abs(delta) / (double) maxIncrement);

            boolean absolute = tuple.getT1().canAbsolute();

            // If the target position is reachable, move there in one command
            if (moves == 1) {
                return moveCmd(absolute ? targetPos : delta);
            }

            Mono<Void> firstMove = moveCmd(absolute ? startPos + maxIncrement : maxIncrement)
                .then(getPosition())
                .repeatWhen(repeat -> repeat.delayElements(Duration.ofMillis(statusUpdateInterval)))
                .takeUntil(actualPos -> Objects.equals(actualPos, startPos + maxIncrement))
                .last()
                .then();

            // Create a stream of moves to execute
            Mono<Void> remainingMoves = Flux
                .range(2, moves-1)
                .concatMap(i -> {
                    int increment = (i >= moves) ? delta % maxIncrement : maxIncrement; // Last move may be smaller than maxIncrement
                    int currTarget = startPos + (maxIncrement*(i-1) + increment);       // Current target position
                    int moveTo = absolute ? currTarget : increment;                     // Parameter for the move command, either absolutely or relatively
                    
                    return moveCmd(moveTo)
                        .then(getPosition())
                        .repeatWhen(repeat -> repeat.delayElements(Duration.ofMillis(statusUpdateInterval)))
                        .takeUntil(actualPos -> Objects.equals(actualPos, currTarget))
                        .last();
                }).then();

            // return firstMove and make remainingMoves wait for firstMove to complete, only returning firstMove's signal as firstMove finishes
            return firstMove.doOnSuccess(v -> remainingMoves.subscribe());
        });
    }

    @Override
    public Mono<Void> moveAwait(int position) throws DeviceException {
        return this.getCapabilities().zipWith(this.getPosition()).flatMap(tuple -> {
            int maxIncrement = tuple.getT1().maxIncrement();
            int maxStep = tuple.getT1().maxStep();
            int startPos = tuple.getT2();
            int targetPos = Math.min(Math.max(0, position), maxStep); // Clamp to limits
            
            // Calculate the number of moves required to reach the target position
            int delta = targetPos - startPos;
            int moves = (int) Math.ceil(Math.abs(delta) / (double) maxIncrement);

            boolean absolute = tuple.getT1().canAbsolute();

            // If the target position is reachable, move there in one command
            if (moves == 1) {
                return moveCmd(absolute ? targetPos : delta)
                    .then(getPosition())
                    .repeatWhen(repeat -> repeat.delayElements(Duration.ofMillis(statusUpdateInterval)))
                    .takeUntil(actualPos -> Objects.equals(actualPos, targetPos))
                    .last()
                    .then();
            }

            // Create a stream of moves and use concatMap to execute the moves sequentially
            return Flux.range(1, moves).concatMap(i -> {
                int increment = (i >= moves) ? delta % maxIncrement : maxIncrement; // Last move may be smaller than maxIncrement
                int currTarget = startPos + (maxIncrement*(i-1) + increment);       // Current target position
                int moveTo = absolute ? currTarget : increment;                     // Parameter for the move command, either absolutely or relatively
                
                return moveCmd(moveTo)
                    .then(getPosition())
                    .repeatWhen(repeat -> repeat.delayElements(Duration.ofMillis(statusUpdateInterval)))
                    .takeUntil(actualPos -> Objects.equals(actualPos, currTarget))
                    .last();
            }).then();
        });
    }

    @Override
    public Mono<Void> moveRelative(int position) {
        return getPosition().flatMap(currPos -> move(currPos + position));
    }

    @Override
    public Mono<Void> moveRelativeAwait(int position) {
        return getPosition().flatMap(currPos -> moveAwait(currPos + position));
    }

    @Override
    public Mono<Void> halt() throws DeviceException {
        return this.put("halt", null).then();
    }

    @Override
    public Mono<Void> setTempComp(boolean enable) throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canTempComp()) {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
                params.add("TempComp", String.valueOf(enable));
                return this.put("tempcomp", params).then();
            } else {
                return Mono.error(new DeviceException("Device does not support temperature compensation"));
            }
        });
    }
    
    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    @Override
    public Mono<FocuserCapabilities> getCapabilities() {
        if (capabilities != null) {
            return Mono.just(capabilities);
        }

        // Load capabilities from the service.
        Mono<Boolean> canAbsolute = this.get("absolute").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canTempComp = this.get("tempcompavailable").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Integer> maxIncrement = this.get("maxincrement").map(JsonNode::asInt).onErrorReturn(-1);
        Mono<Integer> maxStep = this.get("maxstep").map(JsonNode::asInt).onErrorReturn(-1);
        Mono<Integer> stepSize = this.get("stepsize").map(JsonNode::asInt).onErrorReturn(-1);
        
        Mono<FocuserCapabilities> ret = Mono
            .zip(canAbsolute, canTempComp, maxIncrement, maxStep, stepSize)
            .map(tuple -> new FocuserCapabilities(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4(),
                tuple.getT5()
            ));

        // Only run if the device is connected.
        return this.isConnected()
            .flatMap(connected -> Boolean.TRUE.equals(connected) ? ret : Mono.error(new DeviceException("Device is not connected.")))
            .doOnSuccess(caps -> capabilities = caps);
    }

    @Override
    public Mono<FocuserStatus> getStatus() {
        Mono<Boolean> connected = isConnected().onErrorReturn(false);
        Mono<Integer> position = getPosition().onErrorReturn(-1);
        Mono<Double> temperature = getTemperature().onErrorReturn(Double.NaN);
        Mono<Boolean> tempComp = isTempComp().onErrorReturn(false);
        Mono<Boolean> moving = isMoving().onErrorReturn(false);

        return Mono.zip(connected, position, temperature, tempComp, moving).map(tuple ->
            new FocuserStatus(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4(),
                tuple.getT5()
            ));
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

    private Mono<Void> moveCmd(int position) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Position", String.valueOf(position));
        return this.put("move", params).then();
    }

    //#endregion
}
