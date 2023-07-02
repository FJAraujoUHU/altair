package com.aajpm.altair.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.ObservatoryService.ObservatoryStatus;
import com.aajpm.altair.service.observatory.CameraService;
import com.aajpm.altair.service.observatory.DomeService;
import com.aajpm.altair.service.observatory.FilterWheelService;
import com.aajpm.altair.service.observatory.FocuserService;
import com.aajpm.altair.service.observatory.TelescopeService;
import com.aajpm.altair.service.observatory.WeatherWatchService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// TODO: Add Governor panel and controller
// TODO: Add Admin panel with CRUD for all services
@Controller
@RequestMapping("/altair/observatory")
public class ObservatoryController {
    
    /////////////////////////// SUPPORTING SERVICES ///////////////////////////
    //#region SUPPORTING SERVICES

    @Autowired
    ObservatoryService observatory;

    @Autowired
    TelescopeService telescope;

    @Autowired
    DomeService dome;

    @Autowired
    FocuserService focuser;

    @Autowired
    FilterWheelService filterWheel;

    @Autowired
    CameraService camera;

    @Autowired
    WeatherWatchService weatherWatch;

    //#endregion
    //////////////////////////////// ATTRIBUTES ///////////////////////////////
    
    ObservatoryConfig config;
    
    /////////////////////////////// CONSTRUCTORS //////////////////////////////
    
    public ObservatoryController(ObservatoryConfig config) {
        this.config = config;
    }

    //////////////////////////////// ENDPOINTS ////////////////////////////////

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "observatory/dashboard.html";
    }

    @GetMapping("/telescope")
    public Mono<String> telescope(Model model) {
        return telescope
            .getCapabilities()
            .doOnSuccess(capabilities -> model.addAttribute("capabilities", capabilities))
            .onErrorResume(throwable -> {
                model.addAttribute("callError", throwable);
                return Mono.empty();
            })
            .thenReturn("observatory/telescope.html");
    }

    @GetMapping("/dome")
    public Mono<String> dome(Model model) {
        return dome
            .getCapabilities()
            .doOnSuccess(capabilities -> model.addAttribute("capabilities", capabilities))
            .onErrorResume(throwable -> {
                model.addAttribute("callError", throwable);
                return Mono.empty();
            })
            .thenReturn("observatory/dome.html");
    }

    @GetMapping("/focuser")
    public Mono<String> focuser(Model model) {
        return focuser
            .getCapabilities()
            .doOnSuccess(capabilities -> model.addAttribute("capabilities", capabilities))
            .onErrorResume(throwable -> {
                model.addAttribute("callError", throwable);
                return Mono.empty();
            })
            .thenReturn("observatory/focuser.html");
    }

    @GetMapping("/camera")
    public Mono<String> camera(Model model) {
        return camera
            .getCapabilities()
            .doOnSuccess(capabilities -> model.addAttribute("capabilities", capabilities))
            .onErrorResume(throwable -> {
                model.addAttribute("callError", throwable);
                return Mono.empty();
            })
            .thenReturn("observatory/camera.html");
    }

    @GetMapping("/weatherwatch")
    public Mono<String> weatherwatch(Model model) {
        return weatherWatch
            .getCapabilities()
            .doOnSuccess(capabilities -> model.addAttribute("capabilities", capabilities))
            .onErrorResume(throwable -> {
                model.addAttribute("callError", throwable);
                return Mono.empty();
            })
            .thenReturn("observatory/weatherwatch.html");
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ObservatoryStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> observatory.getStatus());
    }
}
