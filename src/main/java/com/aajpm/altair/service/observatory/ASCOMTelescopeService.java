package com.aajpm.altair.service.observatory;

import java.time.Duration;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.aajpm.altair.utility.exception.DeviceException;
import com.aajpm.altair.utility.webutils.AlpacaClient;
import com.fasterxml.jackson.databind.JsonNode;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public class ASCOMTelescopeService extends TelescopeService {

    AlpacaClient client;

    final int deviceNumber;

    final int statusUpdateInterval; // how often checks the state of the device for synchronous methods, in milliseconds

    final long synchronousTimeout; // how long to wait for synchronous methods to complete, in milliseconds

    private TelescopeCapabilities capabilities;


    public ASCOMTelescopeService(AlpacaClient client, int statusUpdateInterval, long synchronousTimeout) {
        this(client, 0, statusUpdateInterval, synchronousTimeout);
    }

    public ASCOMTelescopeService(AlpacaClient client, int deviceNumber, int statusUpdateInterval, long synchronousTimeout) {
        this.client = client;
        this.statusUpdateInterval = statusUpdateInterval;
        this.deviceNumber = deviceNumber;
        this.synchronousTimeout = synchronousTimeout;
        this.getCapabilities().onErrorComplete().subscribe(); // attempt to get the device's capabilities
    }

    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters

    @Override
    public Mono<Boolean> isConnected() {
        return this.get("connected").map(JsonNode::asBoolean);
    }

    @Override
    public Mono<Boolean> isParked() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canPark()) {
                return this.get("atpark").map(JsonNode::asBoolean);
            } else {
                return Mono.error(new DeviceException("Telescope does not support parking."));
            }
        });
    }

    @Override
    public Mono<Boolean> isAtHome() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canFindHome()) {
                return this.get("athome").map(JsonNode::asBoolean);
            } else {
                return Mono.error(new DeviceException("Telescope does not support finding home."));
            }
        });
    }

    @Override
    public Mono<Boolean> isSlewing() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canSlew()) {
                return this.get("slewing").map(JsonNode::asBoolean);
            } else {
                return Mono.error(new DeviceException("Telescope does not support slewing."));
            }
        });
    }

    @Override
    public Mono<Boolean> isTracking() throws DeviceException {
        return getCapabilities().flatMap(caps -> {
            if (caps.canTrack()) {
                return this.get("tracking").map(JsonNode::asBoolean);
            } else {
                return Mono.error(new DeviceException("Telescope does not support tracking."));
            }
        });
    }

    @Override
    public Mono<double[]> getAltAz() throws DeviceException {
        return this
            .get("altitude").map(JsonNode::asDouble)
            .zipWith(this.get("azimuth").map(JsonNode::asDouble))
            .map(tuple -> new double[] { tuple.getT1(), tuple.getT2() });

    }

    @Override
    public Mono<double[]> getCoordinates() throws DeviceException {
        return this
            .get("rightascension").map(JsonNode::asDouble)
            .zipWith(this.get("declination").map(JsonNode::asDouble))
            .map(tuple -> new double[] { tuple.getT1(), tuple.getT2() });
    }

    @Override
    public Mono<Double> getSiderealTime() throws DeviceException {
        return this.get("siderealtime").map(JsonNode::asDouble);
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    @Override
    public Mono<Boolean> connect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "true");
        return this.put("connected", args)
            .doOnSuccess(v -> this.getCapabilities().subscribe())   // attempt to get the device's capabilities
            .thenReturn(true);
    }

    @Override
    public Mono<Boolean> disconnect() throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Connected", "false");
        return this.put("connected", args).thenReturn(true);
    }

    @Override
    public Mono<Boolean> park() throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canPark()) {
                return this.isParked().flatMap(parked -> {
                    if (Boolean.TRUE.equals(parked)) {
                        // Do nothing if already parked
                        return Mono.just(true);
                    } else {
                        return this.put("park", null).thenReturn(true);
                    }
                });
            } else {
                return Mono.error(new DeviceException("Telescope does not support parking."));
            }
        });
    }

    @Override
    public Mono<Boolean> parkAwait() throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canPark()) {
                return this.isParked().flatMap(parked -> {
                    if (Boolean.TRUE.equals(parked)) {
                        // Do nothing if already parked
                        return Mono.just(true);
                    } else {
                        return this.park()
                            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
                            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if parked
                            .flatMap(i -> this.isParked()                                       // keep checking until parked
                                .filter(Boolean.TRUE::equals)
                                .flatMap(p -> Mono.just(true))
                            ).next()
                            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE));
                    }
                });
            } else {
                return Mono.error(new DeviceException("Telescope does not support parking."));
            }
        });
    }

    @Override
    public Mono<Boolean> unpark() throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canPark()) {
                return this.isParked().flatMap(parked -> {
                    if (Boolean.FALSE.equals(parked)) {
                        // Do nothing if already unparked
                        return Mono.just(true);
                    } else {
                        return this.put("unpark", null).thenReturn(true);
                    }
                });
            } else {
                return Mono.error(new DeviceException("Telescope does not support parking."));
            }
        });
    }

    @Override
    public Mono<Boolean> unparkAwait() throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canPark()) {
                return this.isParked().flatMap(parked -> {
                    if (Boolean.FALSE.equals(parked)) {
                        // Do nothing if already unparked
                        return Mono.just(true);
                    } else {
                        return this.unpark()
                            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
                            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if parked
                            .flatMap(i -> this.isParked()                                       // keep checking until parked is false
                                .filter(Boolean.FALSE::equals)
                                .flatMap(p -> Mono.just(true))
                            ).next()
                            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE));
                    }
                }); 
            } else {
                return Mono.error(new DeviceException("Telescope does not support parking."));
            }
        });
    }

    @Override
    public Mono<Boolean> findHome() throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canFindHome()) {
                return this.put("findhome", null).thenReturn(true);
            } else {
                return Mono.error(new DeviceException("Telescope does not support finding home."));
            }
        });
    }

    @Override
    public Mono<Boolean> findHomeAwait() throws DeviceException {
        return findHome()
            .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
            .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if at home
            .flatMap(i -> this.isAtHome()                                       // keep checking until atHome is true
                .filter(Boolean.TRUE::equals)
                .flatMap(atHome -> Mono.just(true))
            ).next()
            .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE));
    }

    @Override
    public Mono<Boolean> slewToCoords(double rightAscension, double declination) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("RightAscension", String.valueOf(rightAscension));
        args.add("Declination", String.valueOf(declination));
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canSlew()) {
                return this.put("slewtocoordinatesasync", args).thenReturn(true);
            } else {
                return Mono.error(new DeviceException("Telescope does not support slewing."));
            }
        });
    }

    @Override
    public Mono<Boolean> slewToCoordsAwait(double rightAscension, double declination) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("RightAscension", String.valueOf(rightAscension));
        args.add("Declination", String.valueOf(declination));
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canSlewAwait()) {
                return this.put("slewtocoordinates", args).thenReturn(true);
            } else if (caps.canSlew()) {
                return this.slewToCoords(rightAscension, declination)
                    .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
                    .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if slewing
                    .flatMap(i -> this.isSlewing()                                      // check until it stops slewing
                        .filter(Boolean.FALSE::equals)
                        .flatMap(slewing -> Mono.just(true))
                    ).next()
                    .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE));
            } else {
                return Mono.error(new DeviceException("Telescope does not support slewing."));
            }
        });
    }

    @Override
    public Mono<Boolean> slewToAltAz(double altitude, double azimuth) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("Altitude", String.valueOf(altitude));
        args.add("Azimuth", String.valueOf(azimuth));
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canSlew()) {
                return this.put("slewtoaltazasync", args).thenReturn(true);
            } else {
                return Mono.error(new DeviceException("Telescope does not support slewing."));
            }
        });
    }

    @Override
    public Mono<Boolean> slewToAltAzAwait(double altitude, double azimuth) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(2);
        args.add("Altitude", String.valueOf(altitude));
        args.add("Azimuth", String.valueOf(azimuth));
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canSlewAwait()) {
                return this.put("slewtoaltaz", args).thenReturn(true);
            } else if (caps.canSlew()) {
                return this.slewToAltAz(altitude, azimuth)
                    .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))          // wait before checking state
                    .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))   // check periodically if slewing
                    .flatMap(i -> this.isSlewing()                                      // check until it stops slewing
                        .filter(Boolean.FALSE::equals)
                        .flatMap(slewing -> Mono.just(true))
                    ).next()
                    .timeout(Duration.ofMillis((synchronousTimeout > 0) ? synchronousTimeout : Long.MAX_VALUE));
            } else {
                return Mono.error(new DeviceException("Telescope does not support slewing."));
            }
        });
    }

    @Override
    public Mono<Boolean> abortSlew() throws DeviceException {
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canSlew()) {
                return this.isSlewing() // Check if slewing, if not, do nothing.
                        .flatMap(slewing -> Boolean.TRUE.equals(slewing)
                            ? put("abortslew", null)
                            : Mono.empty()
                        ).thenReturn(true);
            } else {
                return Mono.error(new DeviceException("Telescope does not support slewing."));
            }
        });
    }

    @Override
    public Mono<Boolean> setTracking(boolean tracking) throws DeviceException {
        MultiValueMap<String, String> args = new LinkedMultiValueMap<>(1);
        args.add("Tracking", String.valueOf(tracking));
        return this.getCapabilities().flatMap(caps -> {
            if (caps.canTrack()) {
                return this.put("tracking", args).thenReturn(true);
            } else {
                return Mono.error(new DeviceException("Telescope does not support tracking."));
            }
        });
    }
    
    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    @Override
    public Mono<TelescopeCapabilities> getCapabilities() {
        if (capabilities != null) {
            return Mono.just(capabilities);
        }

        // Load capabilities from the service.
        Mono<Boolean> canFindHome = this.get("canfindhome").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canPark = this.get("canpark").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canUnpark = this.get("canunpark").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canSlew = this.get("canslewasync").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canSlewAwait = this.get("canslew").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<Boolean> canTrack = this.get("cansettracking").map(JsonNode::asBoolean).onErrorReturn(false);
        Mono<String> name = this.get("name").map(JsonNode::asText).onErrorReturn("ASCOM Telescope");

        Mono<TelescopeCapabilities> ret = Mono
            .zip(canFindHome, canPark, canUnpark, canSlewAwait, canSlew, canTrack, name)
            .map(tuple -> new TelescopeCapabilities(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4(),
                tuple.getT5(),
                tuple.getT6(),
                tuple.getT7()
            ));

        // Only run if the device is connected.
        return this.isConnected()
            .flatMap(connected -> Boolean.TRUE.equals(connected) ? ret : Mono.error(new DeviceException("Device is not connected.")))
            .doOnSuccess(caps -> capabilities = caps);
    }

    @Override
    public Mono<TelescopeStatus> getStatus() {
        Mono<Boolean> connected = this.isConnected().onErrorReturn(false);
        Mono<double[]> altAz = this.getAltAz().onErrorReturn(new double[] { Double.NaN, Double.NaN });
        Mono<double[]> raDec = this.getCoordinates().onErrorReturn(new double[] { Double.NaN, Double.NaN });
        Mono<Boolean> atHome = this.isAtHome().onErrorReturn(false);
        Mono<Boolean> parked = this.isParked().onErrorReturn(false);
        Mono<Boolean> slewing = this.isSlewing().onErrorReturn(false);
        Mono<Boolean> tracking = this.isTracking().onErrorReturn(false);
        Mono<Double> siderealTime = this.getSiderealTime().onErrorReturn(Double.NaN);

        return Mono.zip(connected, altAz, raDec, atHome, parked, slewing, tracking, siderealTime).map(tuple ->
            new TelescopeStatus(
                tuple.getT1(),
                tuple.getT2()[0],
                tuple.getT2()[1],
                tuple.getT3()[0],
                tuple.getT3()[1],
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
        return client.get("telescope", deviceNumber, action);
    }

    private Mono<JsonNode> put(String action, MultiValueMap<String, String> params) {
        return client.put("telescope", deviceNumber, action, params);
    }

    //#endregion
}
