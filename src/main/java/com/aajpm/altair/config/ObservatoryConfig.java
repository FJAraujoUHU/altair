package com.aajpm.altair.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aajpm.altair.service.ASCOMObservatoryService;
import com.aajpm.altair.service.ObservatoryService;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "altair.observatory")
public class ObservatoryConfig {

    // Interval (in ms) to poll the status and update the UI
    private int statusUpdateInterval = 2500;

    private CameraConfig camera;

    @Bean
    public ObservatoryService observatoryService() {
        return new ASCOMObservatoryService("http://localhost:32323/", camera);
    }
    
    public int getStatusUpdateInterval() {
        return statusUpdateInterval;
    }

    public void setStatusUpdateInterval(int statusUpdateInterval) {
        this.statusUpdateInterval = statusUpdateInterval;
    }

    public CameraConfig getCamera() {
        return camera;
    }

    public void setCamera(CameraConfig camera) {
        this.camera = camera;
    }

    public static class CameraConfig {
        // Maximum rate (in °C/min) to cool the camera
        private double maxCooldownRate = 5.0;

        // Minimum rate (in °C/min) to cool the camera. If it cools down too slowly, the cooler will stop cooling further.
        private double minCooldownRate = 0.5;

        // Maximum rate (in °C/min) to warm the camera
        private double maxWarmupRate = 3.0;

        // Power level at which the cooler is considered saturated
        private double coolerSaturationThreshold = 0.9;

        // Size of the image processing buffer, in bytes.
        private int imageBufferSize = -1;

        // Path to store the images
        @NotNull
        private Path imageStorePath = Path.of(System.getProperty("user.home"), "Altair", "images");

        
        @PostConstruct  // Sets up the image store path
        public void init() throws IOException {
            if (!Files.exists(imageStorePath)) {
                Files.createDirectories(imageStorePath);
            } else {
                if (!Files.isDirectory(imageStorePath)) {
                    throw new IOException("Image store path is not a directory: " + imageStorePath);
                }
                if (!Files.isWritable(imageStorePath)) {
                    throw new IOException("Image store path is not writable: " + imageStorePath);
                }
            }
        }

        // Getters/setters
        public double getMaxCooldownRate() {
            return maxCooldownRate;
        }

        public void setMaxCooldownRate(double maxCooldownRate) {
            this.maxCooldownRate = maxCooldownRate;
        }

        public double getMaxWarmupRate() {
            return maxWarmupRate;
        }

        public void setMaxWarmupRate(double maxWarmupRate) {
            this.maxWarmupRate = maxWarmupRate;
        }

        public double getCoolerSaturationThreshold() {
            return coolerSaturationThreshold;
        }

        public void setCoolerSaturationThreshold(double coolerSaturationThreshold) {
            this.coolerSaturationThreshold = coolerSaturationThreshold;
        }

        public double getMinCooldownRate() {
            return minCooldownRate;
        }

        public void setMinCooldownRate(double minCooldownRate) {
            this.minCooldownRate = minCooldownRate;
        }

        public int getImageBufferSize() {
            return imageBufferSize;
        }

        public void setImageBufferSize(int imageBufferSize) {
            this.imageBufferSize = imageBufferSize;
        }

        public Path getImageStorePath() {
            return imageStorePath;
        }

        public void setImageStorePath(String imageStorePath) {
            this.imageStorePath = Path.of(imageStorePath);
        }
    }
}
