package com.aajpm.altair.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.aajpm.altair.service.ObservatoryService;
import com.aajpm.altair.service.observatory.DomeService;

import jakarta.annotation.PostConstruct;

@Controller
@RequestMapping("/altair/observatory/dome")
public class DomeController {
    
    @Autowired
    ObservatoryService observatory;
    
    DomeService dome;
    
    @PostConstruct
    public void init() {
        dome = observatory.getDome();
    }
    
    @GetMapping("")
    public String dashboard() {
        return "observatory/dome.html";
    }
}
