package com.aajpm.altair.controller.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.observatory.WeatherWatchService;
import com.aajpm.altair.service.observatory.WeatherWatchService.WeatherWatchCapabilities;
import com.aajpm.altair.service.observatory.WeatherWatchService.WeatherWatchStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/altair/api/weatherwatch")
public class WeatherWatchAPIController {

    @Autowired
    WeatherWatchService weatherWatch;

    @Autowired
    ObservatoryConfig config;


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<WeatherWatchStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> weatherWatch.getStatus());
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @GetMapping("/getcapabilities")
    public Mono<WeatherWatchCapabilities> getCapabilities() {
        return weatherWatch.getCapabilities();
    }

    //#endregion
    ///////////////////////////////// SETTERS /////////////////////////////////
    //#region Setters

    @PostMapping(value = "/connect")
    public Mono<Void> connect() {
        return weatherWatch.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Void> disconnect() {
        return weatherWatch.disconnect();
    }

    
}
