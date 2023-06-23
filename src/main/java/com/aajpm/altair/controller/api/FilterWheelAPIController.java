package com.aajpm.altair.controller.api;

import java.util.List;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.observatory.FilterWheelService;
import com.aajpm.altair.service.observatory.FilterWheelService.FilterWheelStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/altair/api/filterwheel")
public class FilterWheelAPIController {

    @Autowired
    FilterWheelService filterWheel;

    @Autowired
    ObservatoryConfig config;


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<FilterWheelStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> filterWheel.getStatus());
    }

    @GetMapping(value = "/connected")
    public Mono<Boolean> isConnected() {
        return filterWheel.isConnected();
    }

    @GetMapping(value = "/getfiltercount")
    public Mono<Integer> getFilterCount() {
        return filterWheel.getFilterCount();
    }

    @GetMapping(value = "/getposition")
    public Mono<Integer> getPosition() {
        return filterWheel.getPosition();
    }

    @GetMapping(value = "/getfiltername")
    public Mono<String> getFilterName() {
        return filterWheel.getFilterName();
    }

    @GetMapping(value = "/getfocusoffset")
    public Mono<Integer> getFocusOffset() {
        return filterWheel.getFocusOffset();
    }

    @GetMapping(value = "/getfilternames")
    public Mono<List<String>> getFilterNames() {
        return filterWheel.getFilterNames();
    }

    @GetMapping(value = "/getfocusoffsets")
    public Mono<List<Integer>> getFocusOffsets() {
        return filterWheel.getFocusOffsets();
    }

    @PostMapping(value = "/connect")
    public Mono<Void> connect() {
        return filterWheel.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Void> disconnect() {
        return filterWheel.disconnect();
    }

    @PostMapping(value = "/setposition")
    public Mono<Void> setPosition(int position) {
        return filterWheel.setPosition(position);
    }
}
