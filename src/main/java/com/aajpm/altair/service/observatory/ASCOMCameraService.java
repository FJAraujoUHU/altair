package com.aajpm.altair.service.observatory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.aajpm.altair.config.ObservatoryConfig.CameraConfig;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import nom.tam.fits.Fits;

import com.aajpm.altair.utility.exception.*;


import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

// TODO: test
// TODO: implement image capture
public class ASCOMCameraService extends CameraService {

    AlpacaClient client;

    final int deviceNumber;

    private boolean isWarmingUp = false;
    private boolean isCoolingDown = false;
    private final Logger logger = LoggerFactory.getLogger(ASCOMCameraService.class.getName());

    public ASCOMCameraService(AlpacaClient client, CameraConfig config) {
        super(config);
        this.client = client;
        this.deviceNumber = 0;
    }

    public ASCOMCameraService(AlpacaClient client, int deviceNumber, CameraConfig config) {
        super(config);
        this.client = client;
        this.deviceNumber = deviceNumber;
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Integer> getStatus() {
        return this.get("camerastate").map(JsonNode::asInt);
    }

    @Override
    public Mono<Double> getStatusCompletion() {
        return this.get("perecentcompleted")
            .map(JsonNode::asDouble)
            .onErrorReturn(e -> { // If the current status doesn't have a completion percentage, return NaN
                if (e instanceof ASCOMException) {
                    return (((ASCOMException) e).getErrorCode() == ASCOMException.INVALID_OPERATION);
                } else {
                    return false;
                }
            }, Double.NaN);
    }

    //#region Temperature info

    @Override
    public Mono<Double> getTemperature() {
        return this.get("ccdtemperature").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getTemperatureTarget() {
        //yes, even if it's called "setccdtemperature", it returns the target temperature. ASCOM can be weird.
        return this.get("setccdtemperature").map(JsonNode::asDouble);
    }

    private Mono<Double> getTempDelta() {
        return Mono.zip(getTemperature(), getTemperatureTarget())
            .map(tuples -> Math.abs(tuples.getT2() - tuples.getT1()));
    }

    @Override
    public Mono<Double> getTemperatureAmbient() {
        return this.get("heatsinktemperature").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getCoolerPower() {
        return this.get("coolerpower").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Integer> getCoolerStatus() {
        if (this.isCoolingDown) return Mono.just(CameraService.COOLER_COOLDOWN);
        if (this.isWarmingUp) return Mono.just(CameraService.COOLER_WARMUP);

        Mono<Boolean> isCoolerOn = this.get("cooleron").map(JsonNode::asBoolean);
        Mono<Double> coolerPower = this.getCoolerPower();
        Mono<Double> currentTemp = this.getTemperature();
        Mono<Double> targetTemp = this.getTemperatureTarget();

        return Mono.zip(isCoolerOn, coolerPower, currentTemp, targetTemp).flatMap(tuples -> {
            if (Boolean.FALSE.equals(tuples.getT1())) {
                return Mono.just(CameraService.COOLER_OFF);
            }

            if (tuples.getT2() > config.getCoolerSaturationThreshold()) {
                return Mono.just(CameraService.COOLER_SATURATED);
            }

            if (Math.abs(tuples.getT3() - tuples.getT4()) < 1.1) {
                return Mono.just(CameraService.COOLER_STABLE);
            }
            
            // If the cooler is on, but neither at full power or at the target temperature, it must be warming up or cooling down.
            return Mono.just(CameraService.COOLER_ACTIVE);
        }).onErrorReturn(CameraService.COOLER_ERROR);
    }


    //#endregion


    //#region Exposure parameters

    @Override
    public Mono<Integer> getSubframeWidth() {
        return this.get("numx").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getSubframeHeight() {
        return this.get("numy").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getSubframeStartX() {
        return this.get("startx").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getSubframeStartY() {
        return this.get("starty").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getBinningX() {
        return this.get("binx").map(JsonNode::asInt);
    }

    @Override
    public Mono<Integer> getBinningY() {
        return this.get("biny").map(JsonNode::asInt);
    }

    //#endregion
    

    //#region Sensor info

    public Mono<String> getSensorType() {
        return this.get("sensortype").map(type -> {
            switch (type.asInt()) {
                case 0:
                    return "Monochrome";
                case 1:
                    return "Colour";
                case 2:
                    return "RGGB";
                case 3:
                    return "CMYG";
                case 4:
                    return "CMYG2";
                case 5:
                    return "LRGB";
                default:
                    return "Unknown";
            }
        });
    }

    public Mono<Tuple2<Integer, Integer>> getBayerOffset() {
        Mono<Integer> x = this.get("bayeroffsetx").map(JsonNode::asInt);
        Mono<Integer> y = this.get("bayeroffsety").map(JsonNode::asInt);
        return Mono.zip(x, y);
    }

    //#endregion
    

    //#region Image readout

    @Override
    public Mono<Boolean> isImageReady() {
        return this.get("imageready").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Fits> getImage() {
        
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented yet");
    }


    //#endregion


    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public void connect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(true));
        this.execute("connected", new LinkedMultiValueMap<>());
    }

    @Override
    public void disconnect() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Connected", String.valueOf(false));
        this.execute("connected", new LinkedMultiValueMap<>());
    }


    //#region Cooler

    @Override
    public void setCooler(boolean enable) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("CoolerOn", String.valueOf(enable));
        this.execute("cooleron", params);
    }

    @Override
    public void setTargetTemp(double temperature) {
        if (isCoolingDown) isCoolingDown = false;
        if (isWarmingUp) isWarmingUp = false;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("SetCCDTemperature", String.valueOf(temperature));
        this.execute("setccdtemperature", params);
    }

    @Override
    public void cooldown(double target) {
        Thread t = new Thread(() -> {
            this.setCooler(true);
            this.isWarmingUp = false;
            this.isCoolingDown = true;
            double delta = this.config.getMaxCooldownRate();
            double threshold = this.config.getCoolerSaturationThreshold();
            double minRate = this.config.getMinCooldownRate();
            try {
                while (this.isCoolingDown) {
                    double startTemp = this.getTemperature().block();
                    // Set new target as current temp minus delta, or the target temp if that is closer.
                    double newTarget = (startTemp - target > delta) ? startTemp - delta : target;

                    this.setTargetTemp(newTarget);
                    Thread.sleep(60000);

                    double newTemp = this.getTemperature().block();
                    
                    // If it's saturated and barely cooled, stop cooling further.
                    if (startTemp - newTemp < minRate && this.getCoolerPower().block() > threshold) {
                        this.isCoolingDown = false;
                    }

                    // If it's cooled down enough, stop cooling further.
                    if (newTemp - target < 1.1) {
                        this.isCoolingDown = false;
                    }
                }
            } catch (InterruptedException e) {
                this.logger.error("Error while cooling down", e);
            }
        });
        t.start();
    }

    @Override
    public void warmup() {
        Double target;
        try {
            target = this.getTemperatureAmbient().block();
            if (target == null) {
                target = 25.0;  // Default to 25C if we can't get the ambient temperature.
            }
        } catch (Exception e) {
            target = 25.0;  
        }
        this.warmup(target);
    }

    @Override
    public void warmup(double target) {
        Thread t = new Thread(() -> {
            this.setCooler(true);
            this.isCoolingDown = false;
            this.isWarmingUp = true;
            double delta = this.config.getMaxWarmupRate();
            try {
                while (this.isWarmingUp) {
                    double startTemp = this.getTemperature().block();
                    // Set new target as current temp plus delta, or the target temp if that is closer.
                    double newTarget = (target - startTemp > delta) ? target - startTemp : target;

                    this.setTargetTemp(newTarget);
                    Thread.sleep(60000);

                    double newTemp = this.getTemperature().block();

                    // If it's warm enough, stop warming.
                    if (target - newTemp < 1.1) {
                        this.isWarmingUp = false;
                    }
                }
            } catch (InterruptedException e) {
                this.logger.error("Error while warming up", e);
            }
        });
        t.start();
    }
    

    //#endregion


    //#region Exposure

    @Override
    public void setSubframeStartX(int startX) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("StartX", String.valueOf(startX));
        this.execute("startx", params);
    }

    @Override
    public void setSubframeStartY(int startY) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("StartY", String.valueOf(startY));
        this.execute("starty", params);
    }

    @Override
    public void setSubframeWidth(int width) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("NumX", String.valueOf(width));
        this.execute("numx", params);
    }

    @Override
    public void setSubframeHeight(int height) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("NumY", String.valueOf(height));
        this.execute("numy", params);
    }

    private void setBinningX(int binx) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("BinX", String.valueOf(binx));
        this.execute("binx", params);
    }

    private void setBinningY(int biny) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("BinY", String.valueOf(biny));
        this.execute("biny", params);
    }

    @Override
    public void setBinning(int bin) {
        setBinning(bin, bin);
    }

    @Override
    public void setBinning(int binx, int biny) {
        setBinningX(binx);
        setBinningY(biny);
    }

    @Override
    public void startExposure(double duration, boolean useLightFrame) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("Duration", String.valueOf(duration));
        params.add("Light", String.valueOf(useLightFrame));
        this.execute("startexposure", params);
    }

    @Override
    public void stopExposure() {
        this.execute("stopexposure", null);
    }

    @Override
    public void abortExposure() {
        this.execute("abortexposure", null);
    }

    //#endregion





    //#endregion
    ///////////////////////////////// HELPERS /////////////////////////////////
    //#region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("camera", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("camera", deviceNumber, action, params);
    }

    private void execute(String action, MultiValueMap<String, String> params) {
        client.put("camera", deviceNumber, action, params).subscribe();
    }

    //#endregion
}
