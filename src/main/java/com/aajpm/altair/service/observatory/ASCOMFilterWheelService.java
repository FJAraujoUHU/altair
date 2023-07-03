package com.aajpm.altair.service.observatory;

import com.aajpm.altair.config.ObservatoryConfig.FilterWheelConfig;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


public class ASCOMFilterWheelService extends FilterWheelService {

    AlpacaClient client;

    final int deviceNumber;
    
    final int statusUpdateInterval; // how often checks the state of the device for synchronous methods

    final long synchronousTimeout; // how long to wait for synchronous methods to complete

    List<String> filterNames = null;
    List<Integer> focusOffsets = null;
    int currentPosition = -1;

    public ASCOMFilterWheelService(AlpacaClient client, FilterWheelConfig config, int statusUpdateInterval, long synchronousTimeout) {
        this(client, 0, config, statusUpdateInterval, synchronousTimeout);
    }

    public ASCOMFilterWheelService(AlpacaClient client, int deviceNumber, FilterWheelConfig config, int statusUpdateInterval, long synchronousTimeout) {
        super(config);
        this.client = client;
        this.deviceNumber = deviceNumber;
        this.statusUpdateInterval = statusUpdateInterval;
        this.synchronousTimeout = synchronousTimeout;

        // If there are custom filter names/offsets, use them. Else, use the ones provided by the service.
        if (config != null) {
            if (config.hasCustomFilterNames()) {
                this.filterNames = config.getFilterNames();
            }

            if (config.hasCustomFocusOffsets()) {
                this.focusOffsets = config.getFocusOffsets();
            }
        }

        // Attempt to get current position
        this.isConnected()
            .flatMap(connected -> Boolean.TRUE.equals(connected) ? this.get("position").map(JsonNode::asInt) : Mono.error(new DeviceException("Device is not connected.")))
            .doOnSuccess(position -> currentPosition = position)
            .onErrorComplete()
            .subscribe();
    }


    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Integer> getFilterCount() throws DeviceException {
        if (filterNames == null) {      // if we haven't gotten the filter count yet, get it from the service.
            return this.get("names").map(JsonNode::size);
        } else {                        // else, return the cached value.
            return Mono.just(filterNames.size());
        }
    }

    @Override
    public Mono<String> getFilterName() throws DeviceException {
        if (filterNames == null) {
            return Mono.zip(this.getFilterNames(), this.getPosition()).flatMap(tuple -> {
                List<String> names = tuple.getT1();
                int position = tuple.getT2();
                return Mono.just(names.get(position));
            });
        } else {
            return this.getPosition().map(position -> this.filterNames.get(position));
        }
    }

    @Override
    public Mono<List<String>> getFilterNames() throws DeviceException {
        if (filterNames == null) {      // if we haven't gotten the filter names yet or there are no custom ones, get them from the service.
            return this.get("names").map(listNode -> {
                    List<String> names = new ArrayList<>();
                    listNode.forEach(node -> names.add(node.asText()));
                    return names;
                })
                .doOnSuccess(names -> filterNames = names);
        } else {                        // else, return the cached value.
            return Mono.just(filterNames);
        }
    }

    @Override
    public Mono<Integer> getFocusOffset() throws DeviceException {
        if (this.focusOffsets == null) {
            return Mono.zip(this.getFocusOffsets(), this.getPosition()).flatMap(tuple -> {
                List<Integer> offsets = tuple.getT1();
                int position = tuple.getT2();
                return Mono.just(offsets.get(position));
            });
        } else {
            return this.getPosition().map(position -> this.focusOffsets.get(position));
        }
    }

    @Override
    public Mono<List<Integer>> getFocusOffsets() throws DeviceException {
        if (this.focusOffsets == null) {
            return this.get("focusoffsets")
                .map(listNode -> {
                    List<Integer> offsets = new ArrayList<>();
                    listNode.forEach(node -> offsets.add(node.asInt()));
                    return offsets;
                })
                .doOnSuccess(offsets -> this.focusOffsets = offsets);

        } else {
            return Mono.just(this.focusOffsets);
        }
    }

    @Override
    public Mono<Integer> getPosition() throws DeviceException {
        if (currentPosition == -1) {
            return this.get("position").map(JsonNode::asInt)
                .flatMap(position -> {
                    if (position < 0) {
                        return Mono.error(new DeviceException("Unknown position."));
                    } else {
                        return Mono.just(position);
                    }
                })
                .doOnSuccess(position -> currentPosition = position);
        } else {
            return Mono.just(currentPosition);
        }
    }

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isMoving() throws DeviceException {
        return this.get("position").map(val -> (val.asInt() == -1));
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public Mono<Void> connect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(true));
        return this.put("connected", params)
            .doOnSuccess(v -> this.getPosition().subscribe())
            .then();
    }

    @Override
    public Mono<Void> disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(false));
        return this.put("connected", params).then();
    }

    @Override
    public Mono<Void> setPosition(int position) throws DeviceException {
        if (position < 0 || (this.filterNames != null && position >= this.filterNames.size())) {
            return Mono.error(new IndexOutOfBoundsException(position));
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Position", String.valueOf(position));
        return this.put("position", params)
                .doOnSuccess(v -> currentPosition = position)   // ASCOM returns -1 if it's currently moving, so we can't rely on the response.
                .then();
    }

    @Override
    public Mono<Void> setPositionAwait(int position) throws DeviceException {
        return setPosition(position)
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if moving
            .flatMap(i -> this.isMoving()                                       // check until it stops moving
                .filter(Boolean.FALSE::equals)
                .flatMap(moving -> Mono.just(true))
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    @Override
    public Mono<FilterWheelStatus> getStatus() throws DeviceException {
            Mono<Boolean> connected =   this.isConnected().onErrorReturn(false);
            Mono<Integer> pos =         this.getPosition().onErrorReturn(-1);
            Mono<String> name =         this.getFilterName().onErrorReturn("Unknown");
            Mono<Integer> offset =      this.getFocusOffset().onErrorReturn(0);
            Mono<Boolean> moving =      this.isMoving().onErrorReturn(false);
    
            return Mono.zip(connected, pos, name, offset, moving).map(
                tuple -> new FilterWheelStatus(
                    tuple.getT1(),
                    tuple.getT2(),
                    tuple.getT3(),
                    tuple.getT4(),
                    tuple.getT5()
                )
            );
    }


    //#endregion
     ///////////////////////////////// HELPERS /////////////////////////////////
    //#region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("filterwheel", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("filterwheel", deviceNumber, action, params);
    }

    //#endregion

}
