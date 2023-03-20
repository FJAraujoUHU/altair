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
import com.aajpm.altair.utility.statusreporting.ObservatoryStatus;

import reactor.core.publisher.Flux;

@Controller
@RequestMapping("/altair/observatory")
public class ObservatoryController {
    
    @Autowired
    ObservatoryService observatory;
    
    ObservatoryConfig config;

    @Autowired
    public ObservatoryController(ObservatoryConfig config) {
        this.config = config;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "observatory/dashboard.html";
    }

    @GetMapping("/telescope")
    public String telescope(Model model) {
        return "observatory/telescope.html";
    }

    @GetMapping("/dome")
    public String dome(Model model) {
        return "observatory/dome.html";
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ObservatoryStatus> getStatus() {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> observatory.getStatus());
    }
}
