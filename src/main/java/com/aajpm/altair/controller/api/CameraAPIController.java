package com.aajpm.altair.controller.api;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.entity.AstroImage;
import com.aajpm.altair.entity.ControlOrder;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;
import com.aajpm.altair.service.AstroImageService;
import com.aajpm.altair.service.GovernorService;
import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.CameraService;
import com.aajpm.altair.service.observatory.CameraService.CameraStatus;

import nom.tam.fits.FitsException;

import com.aajpm.altair.service.observatory.CameraService.CameraCapabilities;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/altair/api/camera")
public class CameraAPIController {

    @Autowired
    ObservatoryService observatory;

    @Autowired
    GovernorService governor;

    @Autowired
    AstroImageService astroImageService;

    @Autowired
    ObservatoryConfig config;

    @Autowired
    CameraService camera;

    private final Logger logger = LoggerFactory.getLogger(CameraAPIController.class);


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
    public Mono<Boolean> connect() {
        return camera.connect();
    }

    @PostMapping(value = "/disconnect")
    public Mono<Boolean> disconnect() {
        return camera.disconnect();
    }

    @PostMapping(value = "/cooleron")
    public Mono<Boolean> coolerOn(@RequestParam("enable") boolean enable) {
        return camera.setCooler(enable);
    }

    @PostMapping(value = "/settargettemp")
    public Mono<Boolean> setTargetTemp(@RequestParam("target") double temp) {
        return camera.setTargetTemp(temp);
    }

    @PostMapping(value = "/warmup")
    public Mono<Boolean> warmup(@RequestParam("target") Optional<Double> temp) {
        if (temp.isPresent()) {
            return camera.warmup(temp.get());
        } else {
            return camera.warmup();
        }
    }

    @PostMapping(value = "/cooldown")
    public Mono<Boolean> cooldown(@RequestParam("target") double temp) {
        return camera.cooldown(temp);
    }

    @PostMapping(value = "/setsubframe")
    public Mono<Boolean> setSubframe(@RequestParam("startx") int x, @RequestParam("starty") int y, @RequestParam("width") int width, @RequestParam("height") int height) {
        if (x < 0 || y < 0 || width < 0 || height < 0) {
            return Mono.error(new IllegalArgumentException("Subframe parameters must be positive"));
        }
        return camera.setSubframe(x, y, width, height);
    }

    @PostMapping(value = "/setbinning", params = {"binx","biny"})
    public Mono<Boolean> setBinning(@RequestParam("binx") int binx, @RequestParam("biny") int biny) {
        if (binx < 1 || biny < 1) {
            return Mono.error(new IllegalArgumentException("Binning must be at least 1"));
        }
        return camera.setBinning(binx, biny);
    }

    @PostMapping(value = "/setbinning", params = "binning")
    public Mono<Boolean> setBinning(@RequestParam("binning") int binning) {
        if (binning < 1) {
            return Mono.error(new IllegalArgumentException("Binning must be at least 1"));
        }
        return camera.setBinning(binning);
    }

    @PostMapping(value = "/startexposure")
    public Mono<Boolean> startExposure(@RequestParam("duration") double duration, @RequestParam("lightframe") boolean lightFrame) {
        return camera.startExposure(duration, lightFrame);
    }

    @PostMapping(value = "/stopexposure")
    public Mono<Boolean> stopExposure() {
        return camera.stopExposure();
    }

    @PostMapping(value = "/abortexposure")
    public Mono<Boolean> abortExposure() {
        return camera.abortExposure();
    }

    @GetMapping(value = "/saveimage", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody Mono<ResponseEntity<Resource>> saveImage() {
        AltairUser currentUser = null;
        ControlOrder currentOrder = null;

        currentUser = governor.getCurrentUser();

        if (governor.getCurrentOrder() instanceof ControlOrder) {
            currentOrder = (ControlOrder) governor.getCurrentOrder();
            if (currentUser == null) {
                currentUser = currentOrder.getUser();
            }
        }

        if (currentUser == null) {
            try {
                currentUser = AltairUserService.getCurrentUser();
            } catch (Exception e) {
                logger.warn("Couldn't find the current user", e);
            }   
        }

        ControlOrder finalOrder = currentOrder;

        return observatory
                .saveImage(null, currentUser)
                .doOnSuccess(path -> {
                    try {
                        AstroImage dbImage = astroImageService.create(path);
                            if (finalOrder != null) {
                                dbImage.setControlOrder(finalOrder);
                            }
                            astroImageService.save(dbImage);
                    } catch (FitsException | IOException e) {
                        logger.error("Error saving image", e);
                    }
                }).map(path -> 
                    ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                        .body(new FileSystemResource(path.toFile())));
    }

    @PostMapping(value = "/dumpimage", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public @ResponseBody Mono<ResponseEntity<Resource>> dumpImage() {
        return camera.dumpImage().map(path -> 
                    ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + path.getFileName().toString() + "\"")
                        .body(new FileSystemResource(path.toFile())));
    }

}
