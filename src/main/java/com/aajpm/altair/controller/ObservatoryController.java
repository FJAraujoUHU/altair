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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/altair/observatory")
public class ObservatoryController {
    
    @Autowired
    ObservatoryService observatory;
    
    ObservatoryConfig config;

    //@Autowired
    public ObservatoryController(ObservatoryConfig config) {
        this.config = config;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "observatory/dashboard.html";
    }

    @GetMapping("/telescope")
    public Mono<String> telescope(Model model) {
        return observatory.getTelescope()
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
        return observatory.getDome()
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
        return observatory.getFocuser()
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
        return observatory.getCamera()
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
        return observatory.getWeatherWatch()
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
