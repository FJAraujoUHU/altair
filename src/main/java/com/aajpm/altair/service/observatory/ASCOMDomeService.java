package com.aajpm.altair.service.observatory;

import java.time.Duration;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ASCOMDomeService extends DomeService {
    
    AlpacaClient client;

    final int deviceNumber;

    final int statusUpdateInterval; // how often checks the state of the device for synchronous methods

    final long synchronousTimeout; // how long to wait for synchronous methods to complete

    private DomeCapabilities capabilities;
    

    public ASCOMDomeService(AlpacaClient client, int statusUpdateInterval, long synchronousTimeout) {
        this.client = client;
        this.deviceNumber = 0;
        this.statusUpdateInterval = statusUpdateInterval;
        this.synchronousTimeout = synchronousTimeout;
        this.getCapabilities().onErrorComplete().subscribe(); // attempt to get the device's capabilities
    }

    public ASCOMDomeService(AlpacaClient client, int deviceNumber, int statusUpdateInterval, long synchronousTimeout) {
        this.client = client;
        this.deviceNumber = deviceNumber;
        this.statusUpdateInterval = statusUpdateInterval;
        this.synchronousTimeout = synchronousTimeout;
        this.getCapabilities().onErrorComplete().subscribe(); // attempt to get the device's capabilities
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters
    
    @Override
    public Mono<Double> getAlt() throws DeviceException {
        return this.get("altitude").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Double> getAz() throws DeviceException {
        return this.get("azimuth").map(JsonNode::asDouble);
    }

    @Override
    public Mono<Integer> getShutterStatus() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canShutter()) {
                return this.get("shutterstatus").map(JsonNode::asInt);
            } else {
                return Mono.error(new DeviceException("Dome does not support shutter control"));
            }
        });
    }

    @Override
    public Mono<Boolean> isAtHome() throws DeviceException {
        return this.get("athome").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isParked() throws DeviceException {
        return this.get("atpark").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isShutterOpen() throws DeviceException {
        return this.getShutterStatus().map(status -> status == DomeService.SHUTTER_OPEN);
    }

    @Override
    public Mono<Boolean> isSlaved() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canSlave()) {
                return this.get("slaved").map(JsonNode::asBoolean);
            } else {
                return Mono.error(new DeviceException("Dome does not support slaving"));
            }
        });
    }

    @Override
    public Mono<Boolean> isSlewing() throws DeviceException {
        return this.get("slewing").map(JsonNode::asBoolean);
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public Mono<Void> connect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "true");
        return this.put("connected", args)
            .doOnSuccess(v -> this.getCapabilities().subscribe())   // attempt to get the device's capabilities
            .then();
    }

    @Override
    public Mono<Void> disconnect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "false");
        return this.put("connected", args).then();
    }

    @Override
    public Mono<Void> closeShutter() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
           if (caps.canShutter()) {
               return this.put("closeshutter", null).then();
           } else {
               return Mono.error(new DeviceException("Dome does not support shutter control"));
           } 
        });
    }

    @Override
    public Mono<Void> closeShutterAwait() throws DeviceException {
        return closeShutter()
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if still open
            .flatMap(i -> this.isShutterOpen()                                  // keep checking until it closes
                .filter(Boolean.FALSE::equals)
                .flatMap(open -> Mono.empty())
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }
    
    @Override
    public Mono<Void> findHome() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canFindHome()) {
                return this.put("findhome", null).then();
            } else {
                return Mono.error(new DeviceException("Dome does not support finding home"));
            }
        });
    }

    @Override
    public Mono<Void> findHomeAwait() throws DeviceException {
        return findHome()
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if at home
            .flatMap(i -> this.isAtHome()                                       // keep checking until atHome is true
                .filter(Boolean.TRUE::equals)
                .flatMap(atHome -> Mono.empty())
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }

    @Override
    public Mono<Void> halt() throws DeviceException {
        return this.put("abortslew", null).then();
    }

    @Override
    public Mono<Void> openShutter() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canShutter()) {
                return this.put("openshutter", null).then();
            } else {
                return Mono.error(new DeviceException("Dome does not support shutter control"));
            }
        });
    }

    @Override
    public Mono<Void> openShutterAwait() throws DeviceException {
        return openShutter()
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if open
            .flatMap(i -> this.isShutterOpen()                                  // keep checking until isShutterOpen() is true
                .filter(Boolean.TRUE::equals)
                .flatMap(isOpen -> Mono.empty())
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }

    @Override
    public Mono<Void> park() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canPark()) {
                return this.put("park", null).then();
            } else {
                return Mono.error(new DeviceException("Dome does not support parking"));
            }
        });
    }

    @Override
    public Mono<Void> parkAwait() throws DeviceException {
        return park()
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if parked
            .flatMap(i -> this.isParked()                                       // keep checking until atPark is true
                .filter(Boolean.TRUE::equals)
                .flatMap(parked -> Mono.empty())
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }

    @Override
    public Mono<Void> setAlt(double degrees) throws DeviceException {
        if (degrees < 0) {
            degrees = 0;
        } else if (degrees > 90) {
            degrees = 90;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Altitude", String.valueOf(degrees));

        return getCapabilities().flatMap(caps -> {
            if (caps.canSetAltitude()) {
                return this.put("slewtoaltitude", params).then();
            } else {
                return Mono.error(new DeviceException("Dome does not support setting altitude"));
            }
        });
    }

    @Override
    public Mono<Void> setAltAwait(double degrees) throws DeviceException {
        return setAlt(degrees)
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if it is slewing
            .flatMap(i -> this.isSlewing()                                      // keep checking until it stops slewing
                .filter(Boolean.FALSE::equals)
                .flatMap(alt -> Mono.empty())
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }

    

    @Override
    public Mono<Void> setSlaved(boolean slaved) throws DeviceException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Slaved", String.valueOf(slaved));

        return getCapabilities().flatMap(caps -> {
            if (caps.canSlave()) {
                return this.put("slaved", params).then();
            } else {
                return Mono.error(new DeviceException("Dome does not support slaving"));
            }
        });
    }

    @Override
    public Mono<Void> slew(double az) throws DeviceException {
        az = az % 360;
        if (az < 0) {
            az += 360;
        }
        if (az == 360) {
            az = 0;
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>(1);
        params.add("Azimuth", String.valueOf(az));

        return getCapabilities().flatMap(caps -> {
            if (caps.canSetAzimuth()) {
                return this.put("slewtoazimuth", params).then();
            } else {
                return Mono.error(new DeviceException("Dome does not support slewing"));
            }
        }); 
    }

    @Override
    public Mono<Void> slewAwait(double az) throws DeviceException {
        return slew(az)
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if it is slewing
            .flatMap(i -> this.isSlewing()                                      // keep checking until it stops slewing
                .filter(Boolean.FALSE::equals)
                .flatMap(slewing -> Mono.empty())
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE)) // timeout if it takes too long
            .then();
    }

    @Override
    public Mono<Void> unpark() throws DeviceException {
        // ASCOM does not have an unpark method, so we call findHome instead if available.
        // Else, just do nothing.
        return getCapabilities().flatMap(caps -> {
            if (caps.canFindHome()) {
                return this.findHome().then();
            } else {
                return Mono.empty();
            }
        });
    }

    @Override
    public Mono<Void> unparkAwait() throws DeviceException {
        // ASCOM does not have an unpark method, so we call findHome instead if available.
        // Else, just do nothing.
        return getCapabilities().flatMap(caps -> {
            if (caps.canFindHome()) {
                return this.findHomeAwait().then();
            } else {
                return Mono.empty();
            }
        });
    }

    

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    @Override
    public Mono<DomeCapabilities> getCapabilities() {
        if (capabilities != null) {
            return Mono.just(capabilities);
        }

        // Load capabilities from the service.
        Mono<Boolean> canFindHome = this.get("canfindhome").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canPark = this.get("canpark").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canShutter = this.get("cansetshutter").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canSetAzimuth = this.get("cansetazimuth").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canSetAltitude = this.get("cansetaltitude").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canSlave = this.get("canslave").map(JsonNode::asBoolean).onErrorReturn(false);


        Mono<DomeCapabilities> ret = Mono
            .zip(canFindHome, canPark, canShutter, canSetAzimuth, canSetAltitude, canSlave)
            .map(tuple -> new DomeCapabilities(
                tuple.getT1(),
                tuple.getT2(),
                true,
                tuple.getT3(),
                tuple.getT4(),
                tuple.getT5(),
                tuple.getT6()
            ));

        // Only run if the device is connected.
        return this.isConnected()
            .flatMap(connected -> Boolean.TRUE.equals(connected) ? ret : Mono.error(new DeviceException("Device is not connected.")))
            .doOnSuccess(caps -> capabilities = caps);
    }

    @Override
    public Mono<DomeStatus> getStatus() {
        Mono<Boolean> connected =       isConnected().onErrorReturn(false);
        Mono<Double> az =               getAz().onErrorReturn(Double.NaN);
        Mono<Integer> shutter =         getShutter().map(value -> (int) Math.round(value*100)).onErrorReturn(0);
        Mono<Boolean> atHome =          isAtHome().onErrorReturn(false);
        Mono<Boolean> parked =          isParked().onErrorReturn(false);
        Mono<Boolean> slaved =          isSlaved().onErrorReturn(false);
        Mono<Boolean> slewing =         isSlewing().onErrorReturn(false);
        Mono<String> shutterStatus =    getShutterStatus()
            .map(status -> {
                switch (status) {
                    case SHUTTER_OPEN:
                        return "Open";
                    case SHUTTER_CLOSED:
                        return "Closed";
                    case SHUTTER_OPENING:
                        return "Opening";
                    case SHUTTER_CLOSING:
                        return "Closing";
                    default:
                        return "Error";
            }}).onErrorReturn("Error");

        return Mono.zip(connected, az, shutter, shutterStatus, atHome, parked, slewing, slaved)
                .map(tuple -> new DomeStatus(
                    tuple.getT1(),
                    tuple.getT2(),
                    tuple.getT3(),
                    tuple.getT4(),
                    tuple.getT5(),
                    tuple.getT6(),
                    tuple.getT7(),
                    tuple.getT8()
                ));
    }


    //#endregion
    ///////////////////////////////// HELPERS /////////////////////////////////
    //#region Helpers

    private Mono<JsonNode> get(String action) {
        return client.get("dome", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("dome", deviceNumber, action, params);
    }

    //#endregion
}
