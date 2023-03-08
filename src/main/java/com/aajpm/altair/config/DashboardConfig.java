package com.aajpm.altair.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aajpm.altair.service.ASCOMObservatoryService;
import com.aajpm.altair.service.ObservatoryService;

@Configuration
public class DashboardConfig {

    @Bean
    public ObservatoryService observatoryService() {
        return new ASCOMObservatoryService("http://localhost:32323/");
    }
    
}
