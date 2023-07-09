package com.aajpm.altair.service.observatory;

import java.time.Duration;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.aajpm.altair.config.ObservatoryConfig.FocuserConfig;
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


    public ASCOMFocuserService(AlpacaClient client, FocuserConfig config, int statusUpdateInterval, long synchronousTimeout) {
        this(client, 0, config, statusUpdateInterval, synchronousTimeout);
    }

    public ASCOMFocuserService(AlpacaClient client, int deviceNumber, FocuserConfig config, int statusUpdateInterval, long synchronousTimeout) {
        super(config);
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
    public Mono<Boolean> connect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", String.valueOf(true));
        return this.put("connected", args)
            .doOnSuccess(v -> this.getCapabilities().subscribe())   // attempt to get the device's capabilities
            .thenReturn(true);
    }

    @Override
    public Mono<Boolean> disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Connected", String.valueOf(false));
        return this.put("connected", params).thenReturn(true);
    }


    @Override
    public Mono<Boolean> move(int position) throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            int maxIncrement = caps.maxIncrement();
            int maxStep = caps.maxStep();
            boolean absolute = caps.canAbsolute();
            int targetPos = Math.min(Math.max(0, position), maxStep); // Clamp to limits
            
            Duration timeout = Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE);
            return absolute ? moveUntilAbsolute(targetPos, maxIncrement).timeout(timeout)
                            : moveUntilRelative(targetPos, maxIncrement).timeout(timeout);
        });
    }

    @Override
    public Mono<Boolean> moveAwait(int position) throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            int maxIncrement = caps.maxIncrement();
            int maxStep = caps.maxStep();
            boolean absolute = caps.canAbsolute();
            int targetPos = Math.min(Math.max(0, position), maxStep); // Clamp to limits

            Duration timeout = Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE);
            return absolute ? moveUntilAbsoluteAwait(targetPos, maxIncrement).timeout(timeout)
                            : moveUntilRelativeAwait(targetPos, maxIncrement).timeout(timeout);
        });
    }


    private Mono<Boolean> moveUntilAbsolute(int target, int maxIncrement) {
         return getPosition().flatMap(startPos -> {
            // Calculate the next move
            int delta = target - startPos;

            if (Math.abs(delta) <= config.getPositionTolerance()) {
                // If it's within the tolerance to the target, stop moving
                return Mono.just(true);
            }

            // Move by the maximum increment, or the remaining distance, whichever is smaller
            int moveAmount = Math.min(Math.abs(delta), maxIncrement);
            // Apply the sign of the delta to the next move
            moveAmount = delta < 0 ? -moveAmount : moveAmount;
            // Calculate the next position
            int nextMove = target + moveAmount;


            return moveCmd(nextMove).doOnSuccess(v -> 
                // Recursively call this method until the target is reached,
                // but only as a side effect after completion of the first
                // move command as to not block, since that'd be the same as
                // just calling the await version of this method.
                Mono.delay(Duration.ofMillis(statusUpdateInterval))
                .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if still open
                .flatMap(i -> this.isMoving()                                       // keep checking until it closes
                    .filter(Boolean.FALSE::equals)
                    .flatMap(open -> Mono.just(true))
                ).next()
                .then(moveUntilAbsoluteAwait(target, maxIncrement))
                .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE))
                .subscribe()
            );
        });
    }

    private Mono<Boolean> moveUntilAbsoluteAwait(int target, int maxIncrement) {
        return getPosition().flatMap(startPos -> {
            // Calculate the next move
            int delta = target - startPos;

            if (Math.abs(delta) <= config.getPositionTolerance()) {
                // If it's within the tolerance, stop moving
                return Mono.just(true);
            }

            // Move by the maximum increment, or the remaining distance, whichever is smaller
            int moveAmount = Math.min(Math.abs(delta), maxIncrement);
            // Apply the sign of the delta to the next move
            moveAmount = delta < 0 ? -moveAmount : moveAmount;
            // Calculate the next position
            int nextMove = target + moveAmount;


            return moveCmd(nextMove)
                    .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
                    .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if still open
                    .flatMap(i -> this.isMoving()                                       // keep checking until it closes
                        .filter(Boolean.FALSE::equals)
                        .flatMap(open -> Mono.just(true))
                    ).next()
                    .then(moveUntilAbsolute(target, maxIncrement));  // recursively call this method until the target position is reached
        });
    }

    private Mono<Boolean> moveUntilRelative(int target, int maxIncrement) {
         return getPosition().flatMap(startPos -> {
            // Calculate the next move
            int delta = target - startPos;

            if (Math.abs(delta) <= config.getPositionTolerance()) {
                // If it's within the tolerance, stop moving
                return Mono.just(true);
            }

            // Move by the maximum increment, or the remaining distance, whichever is smaller
            int moveAmount = Math.min(Math.abs(delta), maxIncrement);
            // Apply the sign of the delta to the next move
            moveAmount = delta < 0 ? -moveAmount : moveAmount;
            // Calculate the next position
            int nextMove = moveAmount;


            return moveCmd(nextMove).doOnSuccess(v -> 
                // Recursively call this method until the target is reached,
                // but only as a side effect after completion of the first
                // move command as to not block, since that'd be the same as
                // just calling the await version of this method.
                Mono.delay(Duration.ofMillis(statusUpdateInterval))
                .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if still open
                .flatMap(i -> this.isMoving()                                       // keep checking until it closes
                    .filter(Boolean.FALSE::equals)
                    .flatMap(open -> Mono.just(true))
                ).next()
                .then(moveUntilRelativeAwait(target, maxIncrement))
                .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE))
                .subscribe()
            );
        });
    }

    private Mono<Boolean> moveUntilRelativeAwait(int target, int maxIncrement) {
        return getPosition().flatMap(startPos -> {
            // Calculate the next move
            int delta = target - startPos;

            if (Math.abs(delta) <= config.getPositionTolerance()) {
                // If it's within the tolerance, stop moving
                return Mono.just(true);
            }

            // Move by the maximum increment, or the remaining distance, whichever is smaller
            int moveAmount = Math.min(Math.abs(delta), maxIncrement);
            // Apply the sign of the delta to the next move
            moveAmount = delta < 0 ? -moveAmount : moveAmount;
            // Calculate the next position
            int nextMove = moveAmount;


            return moveCmd(nextMove)
                    .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
                    .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if still open
                    .flatMap(i -> this.isMoving()                                       // keep checking until it closes
                        .filter(Boolean.FALSE::equals)
                        .flatMap(open -> Mono.just(true))
                    ).next()
                    .then(moveUntilRelativeAwait(target, maxIncrement));  // recursively call this method until the target position is reached
        });
    }

    @Override
    public Mono<Boolean> moveRelative(int position) {
        return getPosition().flatMap(currPos -> move(currPos + position));
    }

    @Override
    public Mono<Boolean> moveRelativeAwait(int position) {
        return getPosition().flatMap(currPos -> moveAwait(currPos + position));
    }

    @Override
    public Mono<Boolean> halt() throws DeviceException {
        return this.put("halt", null).thenReturn(true);
    }

    @Override
    public Mono<Boolean> setTempComp(boolean enable) throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canTempComp()) {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
                params.add("TempComp", String.valueOf(enable));
                return this.put("tempcomp", params).thenReturn(true);
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

    private Mono<Boolean> moveCmd(int position) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Position", String.valueOf(position));
        return this.put("move", params).thenReturn(true);
    }

    //#endregion
}
