package com.aajpm.altair.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.aajpm.altair.service.observatory.ASCOMCameraService;
import com.aajpm.altair.service.observatory.ASCOMDomeService;
import com.aajpm.altair.service.observatory.ASCOMFilterWheelService;
import com.aajpm.altair.service.observatory.ASCOMFocuserService;
import com.aajpm.altair.service.observatory.ASCOMTelescopeService;
import com.aajpm.altair.service.observatory.ASCOMWeatherWatchService;
import com.aajpm.altair.service.observatory.CameraService;
import com.aajpm.altair.service.observatory.DomeService;
import com.aajpm.altair.service.observatory.FilterWheelService;
import com.aajpm.altair.service.observatory.FocuserService;
import com.aajpm.altair.service.observatory.TelescopeService;
import com.aajpm.altair.service.observatory.WeatherWatchService;
import com.aajpm.altair.utility.webutils.AlpacaClient;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix = "altair.observatory")
public class ObservatoryConfig {

    ///////////////////////////////// FIELDS //////////////////////////////////
    //#region Fields

    /**
     * Interval (in ms) to poll the status and update the UI.
     * If set too fast, it might hang the server/devices.
     */
    private int statusUpdateInterval = 2500;

    /** 
     * Timeout for synchronous operations. If set too high, failed operations
     * might never finish, and if set too low, some successful operations might
     * get interrupted. Set to 0 to disable timeouts (not recommended).
     */
    private long synchronousTimeout = 60000;

    /**
     * Whether to use the native slaving of the dome to the telescope or let
     * Altair manually slave them together.
     */
    private boolean useNativeSlaving = true;

    /**
     * If true, Altair will not check if the conditions are safe to operate
     * before slewing. This is not recommended, as it might cause damage to the
     * mount. Use only if you really know what you are doing.
     */
    private boolean disableSafetyChecks = false;

    private CameraConfig camera;

    private FilterWheelConfig filterWheel;


    //#region Getters/Setters
    public int getStatusUpdateInterval() {
        return statusUpdateInterval;
    }

    public void setStatusUpdateInterval(int statusUpdateInterval) {
        this.statusUpdateInterval = statusUpdateInterval;
    }

    public long getSynchronousTimeout() {
        return synchronousTimeout;
    }

    public void setSynchronousTimeout(long synchronousTimeout) {
        this.synchronousTimeout = synchronousTimeout;
    }

    public boolean getUseNativeSlaving() {
        return useNativeSlaving;
    }

    public void setUseNativeSlaving(boolean useNativeSlaving) {
        this.useNativeSlaving = useNativeSlaving;
    }

    public boolean getDisableSafetyChecks() {
        return disableSafetyChecks;
    }

    public void setDisableSafetyChecks(boolean disableSafetyChecks) {
        this.disableSafetyChecks = disableSafetyChecks;
    }

    public CameraConfig getCamera() {
        return camera;
    }

    public void setCamera(CameraConfig camera) {
        this.camera = camera;
    }

    public FilterWheelConfig getFilterWheel() {
        return filterWheel;
    }

    public void setFilterWheel(FilterWheelConfig filterWheel) {
        this.filterWheel = filterWheel;
    }

    //#endregion

    //#endregion
    ////////////////////////////////// BEANS //////////////////////////////////
    //#region Beans

    // TODO: Make more modular and configurable from application.yaml (use a factory?)

    @Bean
    public TelescopeService telescopeService() {
        AlpacaClient client = new AlpacaClient("http://localhost:32323/", (int) synchronousTimeout, (int) synchronousTimeout);
        
        return new ASCOMTelescopeService(client, 0, statusUpdateInterval, synchronousTimeout);
    }

    @Bean
    public DomeService domeService() {
        AlpacaClient client = new AlpacaClient("http://localhost:32323/", (int) synchronousTimeout, (int) synchronousTimeout);
        
        return new ASCOMDomeService(client, 0, statusUpdateInterval, synchronousTimeout);
    }

    @Bean
    public FocuserService focuserService() {
        AlpacaClient client = new AlpacaClient("http://localhost:32323/", (int) synchronousTimeout, (int) synchronousTimeout);
        
        return new ASCOMFocuserService(client, 0, statusUpdateInterval, synchronousTimeout);
    }

    @Bean
    public CameraService cameraService() {
        AlpacaClient client = new AlpacaClient("http://localhost:32323/", (int) synchronousTimeout, (int) synchronousTimeout);
        
        return new ASCOMCameraService(client, 0, camera, statusUpdateInterval, synchronousTimeout);
    }

    @Bean
    public FilterWheelService filterWheelService() {
        AlpacaClient client = new AlpacaClient("http://localhost:32323/", (int) synchronousTimeout, (int) synchronousTimeout);
        
        return new ASCOMFilterWheelService(client, 0, filterWheel, statusUpdateInterval, synchronousTimeout);
    }

    @Bean
    public WeatherWatchService weatherService() {
        AlpacaClient client = new AlpacaClient("http://localhost:32323/", (int) synchronousTimeout, (int) synchronousTimeout);
        
        return new ASCOMWeatherWatchService(client, 0);
    }

    //#endregion
    ////////////////////////////// INNER CLASSES //////////////////////////////
    //#region Inner classes

    public static class CameraConfig {
        /**
         * Maximum cooldown rate for the camera, in °C/min,
         * if hardware does not support auto ramping.
         */
        private double maxCooldownRate = 5.0;

        /**
         * Minimum cooldown rate for the camera, in °C/min,
         * if hardware does not support auto ramping.
         * If it cools down too slowly, the cooler will stop cooling it further.
         */
        private double minCooldownRate = 0.5;

        /**
         * Maximum warmup rate for the camera, in °C/min,
         * if hardware does not support auto ramping.
         */
        private double maxWarmupRate = 3.0;

        /**
         * Target temperature for cooling down the camera when in auto mode, in °C.
         */
        private double targetCooling = -10.0;

        /**
         * If the cooler reaches this power percentage, it is considered saturated.
         * Range: 0-100
         */
        private double coolerSaturationThreshold = 90.0;

        /**
         * Size of the image processing buffer, in bytes.
         * Use '-1' to disable the limiter and use the maximum available memory.
         */
        private int imageBufferSize = -1;

        /**
         * Path to the image store directory.
         */
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

        //#region Getters/Setters
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

        public double getTargetCooling() {
            return targetCooling;
        }

        public void setTargetCooling(double targetCooling) {
            this.targetCooling = targetCooling;
        }
        //#endregion
    }

    public static class FilterWheelConfig {

        /**
         * A custom list of names for the filters in the filter wheel.
         * Leave blank to use Service provided ones.
         */
        private List<String> filterNames;

        /**
         * A custom list of focus offsets for the filters in the filter wheel.
         * Leave blank to use Service provided ones.
         */
        private List<Integer> focusOffsets;


        //#region Getters/Setters
        public List<String> getFilterNames() {
            return filterNames;
        }

        public boolean hasCustomFilterNames() {
            return filterNames != null && !filterNames.isEmpty();
        }

        public void setFilterNames(List<String> filterNames) {
            this.filterNames = filterNames;
        }

        public List<Integer> getFocusOffsets() {
            return focusOffsets;
        }

        public void setFocusOffsets(List<Integer> focusOffsets) {
            this.focusOffsets = focusOffsets;
        }

        public boolean hasCustomFocusOffsets() {
            return focusOffsets != null && !focusOffsets.isEmpty();
        }
        //#endregion
    }

    //#endregion
}
