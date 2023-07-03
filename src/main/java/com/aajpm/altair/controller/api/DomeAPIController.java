package com.aajpm.altair.controller.api;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.observatory.DomeService;
import com.aajpm.altair.service.observatory.DomeService.DomeCapabilities;
import com.aajpm.altair.service.observatory.DomeService.DomeStatus;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/altair/api/dome")
public class DomeAPIController {

    @Autowired
    DomeService dome;

    @Autowired
    ObservatoryConfig config;


    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DomeStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> dome.getStatus());
    }

    @GetMapping(value = "/capabilities")
    public Mono<DomeCapabilities> getCapabilities() {
        return dome.getCapabilities();
    }

    @PostMapping(value = "/connect")
    public Mono<Boolean> connect() {
        return dome.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Boolean> disconnect() {
        return dome.disconnect();
    }

    @PostMapping(value = "/park")
    public Mono<Boolean> park() {
        return dome.park();
    }

    @PostMapping(value = "/unpark")
    public Mono<Boolean> unpark() {
        return dome.unpark();
    }

    @PostMapping(value = "/findhome")
    public Mono<Boolean> findHome() {
        return dome.findHome();
    }

    @PostMapping(value = "/abort")
    public Mono<Boolean> abort() {
        return dome.halt();
    }

    @PostMapping(value = "/openshutter")
    public Mono<Boolean> openShutter() {
        return dome.openShutter();
    }

    @PostMapping(value = "/closeshutter")
    public Mono<Boolean> closeShutter() {
        return dome.closeShutter();
    }

    @PostMapping(value = "/slavedome")
    public Mono<Boolean> slaveDome(@RequestParam(value = "enable") boolean enable) {
        return dome.setSlaved(enable);
    }

    @PostMapping(value = "/slew")
    public Mono<Boolean> slew(@RequestParam(value = "az") double az) {
        return dome.slew(az);
    }

    @PostMapping(value = "/slewrelative")
    public Mono<Boolean> slewRelative(@RequestParam(value = "degrees") double degrees) {
        return dome.slewRelative(degrees);
    }

    @PostMapping(value = "/setshutter")
    public Mono<Boolean> setShutter(@RequestParam(value = "amount") double amount) {
        return dome.setShutter(amount);
    }

    @PostMapping(value = "/moveshutter")
    public Mono<Boolean> moveShutter(@RequestParam(value = "amount") double amount) {
        return dome.setAltRelative(amount);
    }
}
