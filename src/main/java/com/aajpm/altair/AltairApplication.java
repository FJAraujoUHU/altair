package com.aajpm.altair;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@SpringBootApplication
@EnableScheduling
@Controller
public class AltairApplication {

	@Autowired
	BuildProperties buildProperties;

	public static void main(String[] args) {
		SpringApplication.run(AltairApplication.class, args);
	}

	@RequestMapping(value={"/index.html", "/", "/index"})
    public String hello(Model model) {
		System.out.println("buildName: " + buildProperties.getName() + " buildVersion: " + buildProperties.getVersion());
		model.addAttribute("buildName", buildProperties.getName());
		model.addAttribute("buildVersion", buildProperties.getVersion());
    	return "index.html";
    }

}
