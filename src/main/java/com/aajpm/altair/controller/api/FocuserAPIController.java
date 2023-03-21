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
import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.FocuserService;
import com.aajpm.altair.utility.statusreporting.FocuserStatus;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/altair/api/focuser")
public class FocuserAPIController {

    @Autowired
    ObservatoryService observatory;

    @Autowired
    ObservatoryConfig config;

    FocuserService focuser;

    @PostConstruct
    public void init() {
        focuser = observatory.getFocuser();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<FocuserStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> focuser.getStatus());
    }

    @PostMapping(value = "/connect")
    public void connect() {
        focuser.connect();
    }

    @PostMapping(value = "/disconnect")
    public void disconnect() {
        focuser.disconnect();
    }

    @PostMapping(value = "/abort")
    public void abort() {
        focuser.halt();
    }

    @PostMapping(value = "/move")
    public void move(@RequestParam(value = "position") int position) {
        focuser.move(position);
    }

    @PostMapping(value = "/moverelative")
    public void moveRelative(@RequestParam(value = "position") int position) {
        focuser.moveRelative(position);
    }

    @PostMapping(value = "/tempcomp")
    public void tempComp(@RequestParam(value = "enable") boolean enable) {
        focuser.setTempComp(enable);
    }
}
