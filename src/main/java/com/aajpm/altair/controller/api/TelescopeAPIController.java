package com.aajpm.altair.controller.api;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.TelescopeService;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.aajpm.altair.utility.statusreporting.TelescopeStatus;

import reactor.core.publisher.Flux;
import java.time.Duration;


@RestController
@RequestMapping("/altair/api/telescope")
public class TelescopeAPIController {

    @Autowired
    ObservatoryService observatory;

    @Autowired
    ObservatoryConfig config;

    TelescopeService telescope;

    @PostConstruct
    public void init() {
        telescope = observatory.getTelescope();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<TelescopeStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> telescope.getStatus());
    }

    @PostMapping(value = "/connect")
    public void connect() {
        telescope.connect();
    }

    @PostMapping(value = "/disconnect")
    public void disconnect() {
        telescope.disconnect();
    }

    @PostMapping(value = "/park")
    public void park() {
        telescope.park();
    }

    @PostMapping(value = "/unpark")
    public void unpark() {
        telescope.unpark();
    }

    @PostMapping(value = "/findhome")
    public void findHome() {
        telescope.findHome();
    }

    @PostMapping(value = "/abortslew")
    public void abortSlew() {
        telescope.abortSlew();
    }

    @PostMapping(value = "/settracking")
    public void setTracking(@RequestParam(value = "tracking") boolean tracking) {
        telescope.setTracking(tracking);
    }

    @PostMapping(value = "/slewrelative")
    public void slewRelative(@RequestParam(value = "direction") int direction, @RequestParam(value = "degrees") double degrees) {
        telescope.slewRelative(degrees, direction);
    }

    @PostMapping(value = "/slewtocoords")
    public void slewTo(@RequestParam(value = "ra") double ra, @RequestParam(value = "dec") double dec) {
        telescope.slewToCoords(ra, dec);
    }

    @PostMapping(value = "/slewtoaltaz")
    public void slewToAltAz(@RequestParam(value = "az") double az, @RequestParam(value = "alt") double alt) {
        telescope.slewToAltAz(alt, az);
    }


    
    
}
