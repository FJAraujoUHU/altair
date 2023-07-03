package com.aajpm.altair.controller.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.observatory.TelescopeService;
import com.aajpm.altair.service.observatory.TelescopeService.TelescopeCapabilities;
import com.aajpm.altair.service.observatory.TelescopeService.TelescopeStatus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;


@RestController
@RequestMapping("/altair/api/telescope")
public class TelescopeAPIController {

    @Autowired
    TelescopeService telescope;

    @Autowired
    ObservatoryConfig config;


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TelescopeStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> telescope.getStatus());
    }

    @GetMapping(value = "/capabilities")
    public Mono<TelescopeCapabilities> getCapabilities() {
        return telescope.getCapabilities();
    }

    @PostMapping(value = "/connect")
    public Mono<Boolean> connect() {
        return telescope.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Boolean> disconnect() {
        return telescope.disconnect();
    }

    @PostMapping(value = "/park")
    public Mono<Boolean> park() {
        return telescope.park();
    }

    @PostMapping(value = "/unpark")
    public Mono<Boolean> unpark() {
        return telescope.unpark();
    }

    @PostMapping(value = "/findhome")
    public Mono<Boolean> findHome() {
        return telescope.findHome();
    }

    @PostMapping(value = "/abortslew")
    public Mono<Boolean> abortSlew() {
        return telescope.abortSlew();
    }

    @PostMapping(value = "/settracking")
    public Mono<Boolean> setTracking(@RequestParam(value = "tracking") boolean tracking) {
        return telescope.setTracking(tracking);
    }

    @PostMapping(value = "/slewrelative")
    public Mono<Boolean> slewRelative(@RequestParam(value = "direction") int direction, @RequestParam(value = "degrees") double degrees) {
        return telescope.slewRelative(degrees, direction);
    }

    @PostMapping(value = "/slewtocoords")
    public Mono<Boolean> slewTo(@RequestParam(value = "ra") double ra, @RequestParam(value = "dec") double dec) {
        return telescope.slewToCoords(ra, dec);
    }

    @PostMapping(value = "/slewtoaltaz")
    public Mono<Boolean> slewToAltAz(@RequestParam(value = "az") double az, @RequestParam(value = "alt") double alt) {
        return telescope.slewToAltAz(alt, az);
    }

}
