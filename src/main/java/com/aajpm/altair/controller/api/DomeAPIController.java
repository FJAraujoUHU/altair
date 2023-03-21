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
import com.aajpm.altair.service.observatory.DomeService;
import com.aajpm.altair.utility.statusreporting.DomeStatus;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/altair/api/dome")
public class DomeAPIController {

    @Autowired
    ObservatoryService observatory;

    @Autowired
    ObservatoryConfig config;

    DomeService dome;

    @PostConstruct
    public void init() {
        dome = observatory.getDome();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DomeStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> dome.getStatus());
    }

    @PostMapping(value = "/connect")
    public void connect() {
        dome.connect();
    }

    @PostMapping(value = "/disconnect")
    public void disconnect() {
        dome.disconnect();
    }

    @PostMapping(value = "/park")
    public void park() {
        dome.park();
    }

    @PostMapping(value = "/unpark")
    public void unpark() {
        dome.unpark();
    }

    @PostMapping(value = "/findhome")
    public void findHome() {
        dome.findHome();
    }

    @PostMapping(value = "/abort")
    public void abort() {
        dome.halt();
    }

    @PostMapping(value = "/openshutter")
    public void openShutter() {
        dome.openShutter();
    }

    @PostMapping(value = "/closeshutter")
    public void closeShutter() {
        dome.closeShutter();
    }

    @PostMapping(value = "/slavedome")
    public void slaveDome(@RequestParam(value = "enable") boolean enable) {
        dome.setSlaved(enable);
    }

    @PostMapping(value = "/slew")
    public void slew(@RequestParam(value = "az") double az) {
        dome.slew(az);
    }

    @PostMapping(value = "/slewrelative")
    public void slewRelative(@RequestParam(value = "degrees") double degrees) {
        dome.slewRelative(degrees);
    }

    @PostMapping(value = "/setshutter")
    public void setShutter(@RequestParam(value = "amount") double amount) {
        dome.setShutter(amount);
    }

    @PostMapping(value = "/moveshutter")
    public void moveShutter(@RequestParam(value = "amount") double amount) {
        dome.setAltRelative(amount);
    }
}
