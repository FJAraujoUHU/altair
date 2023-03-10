package com.aajpm.altair.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.TelescopeService;
import com.aajpm.altair.utility.statusreporting.TelescopeStatus;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;

@Controller
@RequestMapping("/altair/observatory/telescope")
public class TelescopeController {

    @Autowired
    ObservatoryService observatory;

    TelescopeService telescope;

    @PostConstruct
    public void init() {
        telescope = observatory.getTelescope();
    }

    @GetMapping("")
    public String dashboard() {
        return "observatory/telescope.html";
    }

    
    
    // TODO: Implement "Job" class
    // TODO: Implement API Controller to handle info requests and commands to the telescope
    // TODO: Implement views
    
}
