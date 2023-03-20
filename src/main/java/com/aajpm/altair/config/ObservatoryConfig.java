package com.aajpm.altair.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aajpm.altair.service.ASCOMObservatoryService;
import com.aajpm.altair.service.ObservatoryService;

@Configuration
@ConfigurationProperties(prefix = "altair.observatory")
public class ObservatoryConfig {

    // Interval (in ms) to poll the status and update the UI
    private int statusUpdateInterval = 2500;

    @Bean
    public ObservatoryService observatoryService() {
        return new ASCOMObservatoryService("http://localhost:32323/");
    }
    
    public int getStatusUpdateInterval() {
        return statusUpdateInterval;
    }

    public void setStatusUpdateInterval(int statusUpdateInterval) {
        this.statusUpdateInterval = statusUpdateInterval;
    }
}
