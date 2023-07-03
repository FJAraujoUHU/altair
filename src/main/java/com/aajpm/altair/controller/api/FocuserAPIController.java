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
import com.aajpm.altair.service.observatory.FocuserService;
import com.aajpm.altair.service.observatory.FocuserService.FocuserCapabilities;
import com.aajpm.altair.service.observatory.FocuserService.FocuserStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/altair/api/focuser")
public class FocuserAPIController {

    @Autowired
    FocuserService focuser;

    @Autowired
    ObservatoryConfig config;

    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<FocuserStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> focuser.getStatus());
    }

    @GetMapping(value = "/capabilities")
    public Mono<FocuserCapabilities> getCapabilities() {
        return focuser.getCapabilities();
    }

    @PostMapping(value = "/connect")
    public Mono<Boolean> connect() {
        return focuser.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Boolean> disconnect() {
        return focuser.disconnect();
    }

    @PostMapping(value = "/abort")
    public Mono<Boolean> abort() {
        return focuser.halt();
    }

    @PostMapping(value = "/move")
    public Mono<Boolean> move(@RequestParam(value = "position") int position) {
        return focuser.move(position);
    }

    @PostMapping(value = "/moverelative")
    public Mono<Boolean> moveRelative(@RequestParam(value = "steps") int steps) {
        return focuser.moveRelative(steps);
    }

    @PostMapping(value = "/tempcomp")
    public Mono<Boolean> tempComp(@RequestParam(value = "enable") boolean enable) {
        return focuser.setTempComp(enable);
    }
}
