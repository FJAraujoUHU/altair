package com.aajpm.altair.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.TelescopeService;

import jakarta.annotation.PostConstruct;

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
