package com.aajpm.altair.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.utility.statusreporting.ObservatoryStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/altair/observatory")
public class ObservatoryController {
    
    @Autowired
    ObservatoryService observatory;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "observatory/dashboard.html";
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<ObservatoryStatus> getStatus() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
