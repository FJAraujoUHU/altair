package com.aajpm.altair.controller.api;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.CameraService;
import com.aajpm.altair.service.observatory.CameraService.CameraStatus;
import com.aajpm.altair.service.observatory.CameraService.CameraCapabilities;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/altair/api/camera")
public class CameraAPIController {

    @Autowired
    ObservatoryService observatory;

    @Autowired
    ObservatoryConfig config;

    CameraService camera;

    @PostConstruct
    public void init() {
        camera = observatory.getCamera();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CameraStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> camera.getStatus());
    }

    @GetMapping(value = "/capabilities")
    public Mono<CameraCapabilities> getCapabilities() {
        return camera.getCapabilities();
    }


    @PostMapping(value = "/connect")
    public Mono<Void> connect() {
        return camera.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Void> disconnect() {
        return camera.disconnect();
    }

    @PostMapping(value = "/cooleron")
    public Mono<Void> coolerOn(@RequestParam("enable") boolean enable) {
        return camera.setCooler(enable);
    }

    @PostMapping(value = "/settargettemp")
    public Mono<Void> setTargetTemp(@RequestParam("target") double temp) {
        return camera.setTargetTemp(temp);
    }

    @PostMapping(value = "/warmup")
    public Mono<Void> warmup(@RequestParam("target") Optional<Double> temp) {
        if (temp.isPresent()) {
            return camera.warmup(temp.get());
        } else {
            return camera.warmup();
        }
    }

    @PostMapping(value = "/cooldown")
    public Mono<Void> cooldown(@RequestParam("target") double temp) {
        return camera.cooldown(temp);
    }

    @PostMapping(value = "/setsubframe")
    public Mono<Void> setSubframe(@RequestParam("startx") int x, @RequestParam("starty") int y, @RequestParam("width") int width, @RequestParam("height") int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            return Mono.error(new IllegalArgumentException("Subframe parameters must be positive"));
        }
        return camera.setSubframe(x, y, width, height);
    }

    @PostMapping(value = "/setbinning", params = {"binx","biny"})
    public Mono<Void> setBinning(@RequestParam("binx") int binx, @RequestParam("biny") int biny) {
        if (binx < 1 || biny < 1) {
            return Mono.error(new IllegalArgumentException("Binning must be at least 1"));
        }
        return camera.setBinning(binx, biny);
    }

    @PostMapping(value = "/setbinning", params = "binning")
    public Mono<Void> setBinning(@RequestParam("binning") int binning) {
        if (binning < 1) {
            return Mono.error(new IllegalArgumentException("Binning must be at least 1"));
        }
        return camera.setBinning(binning);
    }

    @PostMapping(value = "/startexposure")
    public Mono<Void> startExposure(@RequestParam("duration") double duration, @RequestParam("lightframe") boolean lightFrame) {
        return camera.startExposure(duration, lightFrame);
    }

    @PostMapping(value = "/stopexposure")
    public Mono<Void> stopExposure() {
        return camera.stopExposure();
    }

    @PostMapping(value = "/abortexposure")
    public Mono<Void> abortExposure() {
        return camera.abortExposure();
    }

    // TODO : Tweak it so it's easier to use
    @PostMapping(value = "/saveimage")
    public void saveImage(@RequestParam("filename") String filename) {
        camera.saveImage(filename, false);
    }

    @PostMapping(value = "/dumpimage")
    public void dumpImage(@RequestParam("filename") String filename) {
        camera.dumpImage(filename);
    }

}
