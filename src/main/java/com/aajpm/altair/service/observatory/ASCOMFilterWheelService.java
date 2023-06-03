package com.aajpm.altair.service.observatory;

import com.aajpm.altair.config.ObservatoryConfig.FilterWheelConfig;
import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


public class ASCOMFilterWheelService extends FilterWheelService {

    AlpacaClient client;

    final int deviceNumber;

    List<String> filterNames = null;
    List<Integer> focusOffsets = null;
    Integer filterCount = null;

    public ASCOMFilterWheelService(AlpacaClient client, FilterWheelConfig config) {
        this(client, 0, config);
    }

    public ASCOMFilterWheelService(AlpacaClient client, int deviceNumber, FilterWheelConfig config) {
        super(config);
        this.client = client;
        this.deviceNumber = deviceNumber;

        // If there are custom filter names/offsets, use them. Else, use the ones provided by the service.
        if (config != null) {
            if (config.hasCustomFilterNames()) {
                this.filterNames = config.getFilterNames();
            }

            if (config.hasCustomFocusOffsets()) {
                this.focusOffsets = config.getFocusOffsets();
            }
        }
    }


    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Integer> getFilterCount() throws DeviceException {
        if (filterCount == null) {      // if we haven't gotten the filter count yet, get it from the service.
            return this.get("names")
                .map(JsonNode::size)
                .doOnSuccess(count -> filterCount = count);
        } else {                        // else, return the cached value.
            return Mono.just(filterCount);
        }
    }

    @Override
    public Mono<String> getFilterName() throws DeviceException {
        if (this.filterNames == null) {
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
            return this.get("names")
                .map(listNode -> {
                    List<String> names = new ArrayList<>();
                    listNode.forEach(node -> names.add(node.asText()));
                    return names;
                })
                .doOnSuccess(names -> {
                    filterNames = names;
                    filterCount = names.size();
                });
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
        return this.get("position").map(JsonNode::asInt);
    }

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public void connect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(true));
        this.execute("connected", params);
    }

    @Override
    public void disconnect() throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(false));
        this.execute("connected", params);
    }

    @Override
    public void setPosition(int position) throws DeviceException {
        if (position < 0 || (this.filterCount != null && position >= this.filterCount)) {
            throw new IndexOutOfBoundsException(position);
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Position", String.valueOf(position));
        this.execute("position", params);
    }

    @Override
    public void setPositionAwait(int position) throws DeviceException {
        setPosition(position);
        
        Integer lastPos = getPosition()
            .repeatWhen(repeat -> repeat
                .delayElements(java.time.Duration.ofMillis(1000))
                .takeUntil(pos -> pos != -1)
            ).blockLast(java.time.Duration.ofSeconds(20));

        if (lastPos == null || lastPos != position) {
            throw new DeviceException("Filter wheel did not move to position " + position + " in time.");
        }
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

    private void execute(String action, MultiValueMap<String, String> params) {
        client.put("filterwheel", deviceNumber, action, params).subscribe();
    }

    //#endregion

}
