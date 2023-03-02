package com.aajpm.altair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

@SpringBootApplication
@EnableScheduling
@Controller
public class AltairApplication {

	public static void main(String[] args) {
		SpringApplication.run(AltairApplication.class, args);
	}

	@RequestMapping("/index.html")
    public String hello() {
		
      return "index.html";
    }

}
