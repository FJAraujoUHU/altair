package com.aajpm.altair.controller.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.aajpm.altair.service.AstroObjectService;

@Controller
@RequestMapping("/altair/data/astro-object")
public class AstroObjectController {

    @Autowired
    AstroObjectService astroObjectService;

    @GetMapping("/list")
    public String listAll(Model model) {
        model.addAttribute("astroObjects", astroObjectService.findAll());
        
        return "data/astro-object/list";
    }
    
}
