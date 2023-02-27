package com.aajpm.altair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication

@EnableScheduling
@RestController	//doesn't return HTML, but rather a string
public class AltairApplication {

	public static void main(String[] args) {
		SpringApplication.run(AltairApplication.class, args);
	}

	@GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		
      return String.format("Hello %s!", name);
    }

}
