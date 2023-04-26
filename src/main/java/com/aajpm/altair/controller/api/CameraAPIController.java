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
import com.aajpm.altair.utility.statusreporting.CameraStatus;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

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

    @PostMapping(value = "/connect")
    public void connect() {
        camera.connect();
    }

    @PostMapping(value = "/disconnect")
    public void disconnect() {
        camera.disconnect();
    }

    @PostMapping(value = "/cooleron")
    public void coolerOn(@RequestParam("enable") boolean enable) {
        camera.setCooler(enable);
    }

    @PostMapping(value = "/settargettemp")
    public void setTargetTemp(@RequestParam("target") double temp) {
        camera.setTargetTemp(temp);
    }

    @PostMapping(value = "/warmup")
    public void warmup(@RequestParam("target") Optional<Double> temp) {
        if (temp.isPresent()) {
            camera.warmup(temp.get());
        } else {
            camera.warmup();
        }
    }

    @PostMapping(value = "/cooldown")
    public void cooldown(@RequestParam("target") double temp) {
        camera.cooldown(temp);
    }

    @PostMapping(value = "/setsubframe")
    public void setSubframe(@RequestParam("startx") int x, @RequestParam("starty") int y, @RequestParam("width") int width, @RequestParam("height") int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            throw new IllegalArgumentException("Subframe parameters must be positive");
        }
        camera.setSubframe(x, y, width, height);
    }

    @PostMapping(value = "/setbinning", params = {"binx","biny"})
    public void setBinning(@RequestParam("binx") int binx, @RequestParam("biny") int biny) {
        if (binx < 1 || biny < 1) {
            throw new IllegalArgumentException("Binning must be at least 1");
        }
        camera.setBinning(binx, biny);
    }

    @PostMapping(value = "/setbinning", params = "binning")
    public void setBinning(@RequestParam("binning") int binning) {
        if (binning < 1) {
            throw new IllegalArgumentException("Binning must be at least 1");
        }
        camera.setBinning(binning);
    }

    @PostMapping(value = "/startexposure")
    public void startExposure(@RequestParam("duration") double duration, @RequestParam("lightframe") boolean lightFrame) {
        camera.startExposure(duration, lightFrame);
    }

    @PostMapping(value = "/stopexposure")
    public void stopExposure() {
        camera.stopExposure();
    }

    @PostMapping(value = "/abortexposure")
    public void abortExposure() {
        camera.abortExposure();
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
