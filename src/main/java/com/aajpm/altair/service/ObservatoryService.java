package com.aajpm.altair.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aajpm.altair.config.AstrometricsConfig;
import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.entity.AstroObject;
import com.aajpm.altair.entity.ExposureParams;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.utility.exception.*;
import com.aajpm.altair.utility.solver.EphemeridesSolver;

import nom.tam.fits.Fits;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageHDU;
import nom.tam.util.FitsOutputStream;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import com.aajpm.altair.service.observatory.*;
import com.aajpm.altair.service.observatory.CameraService.CameraStatus;
import com.aajpm.altair.service.observatory.DomeService.DomeCapabilities;
import com.aajpm.altair.service.observatory.DomeService.DomeStatus;
import com.aajpm.altair.service.observatory.FilterWheelService.FilterWheelStatus;
import com.aajpm.altair.service.observatory.FocuserService.FocuserStatus;
import com.aajpm.altair.service.observatory.TelescopeService.TelescopeCapabilities;
import com.aajpm.altair.service.observatory.TelescopeService.TelescopeStatus;
import com.aajpm.altair.service.observatory.WeatherWatchService.WeatherWatchCapabilities;
import com.aajpm.altair.service.observatory.WeatherWatchService.WeatherWatchStatus;


@Service
public class ObservatoryService {

    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    protected final ObservatoryConfig config;

    protected final AtomicBoolean useAltairSlaving = new AtomicBoolean(false);

    protected final AtomicBoolean altairSlaved = new AtomicBoolean(false);

    private final Logger logger = LoggerFactory.getLogger(ObservatoryService.class.getName());

    //#endregion
    /////////////////////////// SUPPORTING SERVICES ////////////////////////////
    //#region Supporting services

    @Autowired
    private EphemeridesSolver ephemeridesSolver;

    @Autowired
    private TelescopeService telescope;

    @Autowired
    private DomeService dome;

    @Autowired
    private FocuserService focuser;

    @Autowired
    private FilterWheelService filterWheel;

    @Autowired
    private CameraService camera;

    @Autowired
    private WeatherWatchService weatherWatch;

    //#endregion
    /////////////////////////////// CONSTRUCTOR ///////////////////////////////
    //#region Constructor

    public ObservatoryService(ObservatoryConfig config) {
        super();
        this.config = config;
        useAltairSlaving.set(!config.getUseNativeSlaving());
    }

    //#endregion
    ///////////////////////////////// GETTERS /////////////////////////////////
    //#region Getters


    /**
     * Gets the current configuration of the observatory.
     * @return The current configuration of the observatory.
     */
    public ObservatoryConfig getConfig() { return config; }


    /**
     * Checks if the weather and daylight conditions are safe for observing.
     * 
     * @return A {@link Mono} that will complete with a {@link Boolean} indicating
     *        if the weather and daylight conditions are safe for observing.
     */
    public Mono<Boolean> isSafe() {
        return isSafe(true);
    }

    /**
     * Checks if the weather and daylight conditions are safe for observing.
     * 
     * @param checkDaylight If true, it will check if it is night time. Else
     *                      it will only check the weather conditions.
     * 
     * @return A {@link Mono} that will complete with a {@link Boolean} indicating
     *        if the weather and daylight conditions are safe for observing.
     */
    public Mono<Boolean> isSafe(boolean checkDaylight) {
        Mono<Boolean> weatherSafe = weatherWatch.isConnected()
                                        .flatMap(conn -> Boolean.TRUE.equals(conn)
                                                        ? weatherWatch.isSafe()
                                                        : Mono.just(false))
                                        .onErrorReturn(false);

        Mono<Boolean> daylightSafe = ephemeridesSolver.getNightTime()
                                        .map(nightTime -> nightTime.contains(Instant.now()))
                                        .onErrorReturn(false);

        if (!checkDaylight) return weatherSafe;
        else return Mono.zip(weatherSafe, daylightSafe)
                        .map(tuple -> tuple.getT1() && tuple.getT2());
    }

    /**
     * Checks if the dome is currently slaved to the telescope.
     * @return A {@link Mono} that will complete with a {@link Boolean} indicating
     *         if the dome is currently slaved to the telescope.
     */
    public Mono<Boolean> isSlaved() {
        if (useAltairSlaving.get())
            return Mono.just(altairSlaved.get());
        else
            return dome.isConnected().flatMap(conn -> Boolean.TRUE.equals(conn)
                                                        ? dome.isSlaved()
                                                        : Mono.just(false));
    }

    /**
     * Checks who is currently slaving the dome.
     * @return A {@link Mono} that will complete with a {@link Boolean} that will
     *         be true if Altair is manually slaving the dome, or false if it's
     *         using the dome's native slaving.
     */
    public Mono<Boolean> isAltairSlaving() {
        return Mono.just(useAltairSlaving.get());
    }


    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Connects all the devices of the observatory.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Boolean> connectAll() {
        return Mono.whenDelayError(
            telescope.connect(),
            dome.connect(),
            focuser.connect(),
            filterWheel.connect(),
            camera.connect(),
            weatherWatch.connect()
        ).thenReturn(true)
        .doOnSuccess(s -> logger.info("Observatory::connectAll(): All devices connected successfully"))
        .doOnError(e -> logger.error("Observatory::connectAll(): Error connecting all devices", e));
    }

    /**
     * Disconnects all the devices of the observatory.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Boolean> disconnectAll() {
        return Mono.whenDelayError(
            telescope.disconnect(),
            dome.disconnect(),
            focuser.disconnect(),
            filterWheel.disconnect(),
            camera.disconnect(),
            weatherWatch.disconnect()
        ).thenReturn(true)
        .doOnSuccess(s -> logger.info("Observatory::disconnectAll(): All devices disconnected successfully"))
        .doOnError(e -> logger.error("Observatory::disconnectAll(): Error disconnecting all devices", e));
    }

    /**
     * Disconnects all the devices of the observatory, except the weather watch.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Boolean> disconnectAllExceptWeather() {
        return Mono.whenDelayError(
            telescope.disconnect(),
            dome.disconnect(),
            focuser.disconnect(),
            filterWheel.disconnect(),
            camera.disconnect()
        ).thenReturn(true)
        .doOnSuccess(s -> logger.info("Observatory::disconnectAllExceptWeather(): Devices disconnected successfully"))
        .doOnError(e -> logger.error("Observatory::disconnectAllExceptWeather(): Error disconnecting devices", e));
    }
    
    /**
     * Connects the telescope and dome and enables/disables dome slaving.
     * 
     * @param slaving If true, the dome will be slaved to the telescope.
     *                If false, the dome will be free to move independently.
     * 
     * @return A {@link Mono} that will complete as soon as the changes apply.
     * @throws DeviceException If it's using Native mode and {@code slaving == true}
     *                          and the dome does not support slaving, or the
     *                          device is not connected. This exception will be
     *                          thrown as a {@link Mono#error(Throwable)}.
     */
    public Mono<Boolean> setSlaved(boolean slaving) throws DeviceException {
        return Mono.when(dome.connect(), telescope.connect()).then(
            dome.getCapabilities()
                .flatMap(caps -> {
                    if (useAltairSlaving.get()) {
                        Mono<Boolean> action = Mono.just(true);

                        // If using Altair, make sure native slaving is disabled
                        if (caps.canSlave())
                            action = dome.setSlaved(false);

                        action = action.then(Mono.fromRunnable(() -> 
                                            altairSlaved.set(slaving)))
                                        .thenReturn(true);

                        return action;
                    } else {
                        // If using native, make sure the Slaver is disabled
                        altairSlaved.set(false);

                        // Failsafe: If the dome does not support slaving, but it's
                        // trying to disable slaving, just do nothing
                        if (!caps.canSlave() && !slaving) {
                            return Mono.just(true);
                        }

                        return dome.setSlaved(slaving);
                    }
                })
        )
        .doOnSuccess(s -> logger.info("Observatory::setSlaved({}): Slaving set successfully", slaving))
        .doOnError(e -> logger.error("Observatory::setSlaved(): Error setting dome slaving", e));
    }

    /**
     * Connects the telescope and dome and enables/disables dome slaving.
     * 
     * @param slaving If true, the dome will be slaved to the telescope.
     *                If false, the dome will be free to move independently.
     * 
     * @return A {@link Mono} that will complete after the dome has synced to
     *         the telescope.
     */
    public Mono<Boolean> setSlavedAwait(boolean slaving) {
        if (!slaving)   // If unslaving, just unslave, no need to wait
            return setSlaved(false);

        long statusUpdateInterval = config.getStatusUpdateInterval();
        long synchronousTimeout = config.getSynchronousTimeout();
        synchronousTimeout = synchronousTimeout > 0 ? synchronousTimeout : 300000;

        if (!useAltairSlaving.get()) {
            // If using Native slaving, let it do its thing and wait for it to sync
            return setSlaved(true)
                    .then(Mono.delay(Duration.ofMillis(statusUpdateInterval)))
                    .thenMany(Flux.interval(Duration.ofMillis(statusUpdateInterval)))
                    .flatMap(i -> areTelescopeAndDomeInSync()
                        .filter(Boolean.TRUE::equals)
                        .flatMap(synched -> Mono.just(true))
                    ).next()
                    .timeout(Duration.ofMillis(synchronousTimeout));
        } else {
            // If using the Slaver, disable native then slew dome's azimuth to telescope's
            // and enable the Slaver
            
            return dome.getCapabilities().zipWith(telescope.getAltAz()).flatMap(tuple -> {
                DomeCapabilities caps = tuple.getT1();
                double[] altAz = tuple.getT2();

                Mono<Boolean> action = Mono.just(true);

                if (caps.canSlave()) {
                    // Make sure native slaving is disabled
                    action = dome.setSlaved(false);
                }

                // Slew dome to telescope's azimuth
                if (altAz != null && altAz[1] != Double.NaN) {
                    action = action.then(dome.slewAwait(altAz[1]));
                }

                action = action.then(Mono.fromRunnable(() ->  
                                    altairSlaved.set(true)
                                )).thenReturn(true);

                return action;
            })
            .doOnSuccess(s -> logger.info("Observatory::setSlavedAwait({}): Slaving set successfully", slaving))
            .doOnError(e -> logger.error("Observatory::setSlavedAwait(): Error setting dome slaving", e));
        }
    }

    /** Check if the dome is pointing at the same azimuth as the telescope */
    private Mono<Boolean> areTelescopeAndDomeInSync() {
        return Mono.zip(dome.getCapabilities(), dome.getStatus(), telescope.getStatus())
                    .map(tuple -> {
                        DomeCapabilities caps = tuple.getT1();
                        DomeStatus dmStatus = tuple.getT2();
                        TelescopeStatus tsStatus = tuple.getT3();

                        if (!(dmStatus.connected() && tsStatus.connected())) {
                            return false;
                        }

                        if (!caps.canSetAzimuth()) {
                            return true; // If the dome can't set azimuth, suppose is in sync
                        }
                        
                        double tsAz = tsStatus.azimuth();
                        double dmAz = dmStatus.azimuth();

                        return Math.abs(tsAz - dmAz) < 5; // 5 degrees of tolerance
                    });
    } 

    /**
     * Sets the slaving mode of the dome.
     * 
     * @param useAltairSlaving If true, Altair will periodically sync the dome's
     *                          position to the telescope. Otherwise, it will use
     *                          the dome's native slaving.
     * @return A {@link Mono} that will complete as soon as the changes apply.
     */
    public Mono<Boolean> useAltairSlaving(boolean useAltairSlaving) {
        Mono<DomeCapabilities> domeCapabilities = dome.getCapabilities();
        Mono<Boolean> domeConnected = dome.isConnected();

        return Mono.zip(domeCapabilities, domeConnected)
                    .flatMap(tuple -> {
                        DomeCapabilities caps = tuple.getT1();
                        boolean isDomeConnected = Boolean.TRUE.equals(tuple.getT2());

                        if(isDomeConnected && caps.canSlave()) {
                            return dome.setSlaved(!useAltairSlaving);
                        }
                        return Mono.just(true);
                    }).doOnTerminate(() -> this.useAltairSlaving.set(useAltairSlaving));
    }


    /**
     * Starts the observatory and sets it up ready for use asynchronously.
     * It should unpark the telescope and dome, set them at their home positions,
     * and start chilling the camera.
     * 
     * Note: This method will not open the dome nor slave it to the telescope.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible.
     */
    public Mono<Boolean> start() throws DeviceUnavailableException {
        return connectAll().then(
                    Mono.whenDelayError(
                        startTelescope(),
                        startDome(),
                        startCamera())
                    .thenReturn(true)
                )
                .doOnSuccess(s -> logger.info("Observatory::start(): Observatory started successfully"))
                .doOnError(e -> logger.error("Observatory::start(): Error starting observatory", e));
    }

    /**
     * Starts the telescope and sets it up ready for use.
     * 
     * @return A {@link Mono} that will complete when the telescope has received the command.
     */
    public Mono<Boolean> startTelescope() {
        // If the telescope is not connected, connect it
        Mono<Boolean> ret = telescope.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : telescope.connect()
                                    );
        
        // If the telescope is parked and can unpark, unpark it
        ret = ret.then(
                telescope.getCapabilities()
                    .zipWith(telescope.isParked().onErrorReturn(false))
                    .flatMap(tuples -> {
                        boolean shouldUnpark = tuples.getT1().canUnpark() && tuples.getT2();
                        return shouldUnpark ? telescope.unpark() : Mono.just(true);
                    }));
        
        // If the telescope can track, disable tracking
        ret = ret.then(
                telescope.getCapabilities()
                    .zipWith(telescope.isTracking().onErrorReturn(false))
                    .flatMap(tuples -> {
                        boolean canTrack = tuples.getT1().canTrack();
                        boolean isTracking = tuples.getT2();
                        return canTrack && isTracking ? telescope.setTracking(false) : Mono.just(true);
                    }));
                    
        // If the telescope can find home and is not at home, find home
        ret = ret.then(
                telescope.getCapabilities()
                    .zipWith(telescope.isAtHome().onErrorReturn(true))
                    .flatMap(tuples -> {
                        boolean canFindHome = tuples.getT1().canFindHome();
                        boolean isAtHome = tuples.getT2();
                        return canFindHome && !isAtHome ? telescope.findHome() : Mono.just(true);
                    }));

        return ret.doOnSuccess(s -> logger.info("Observatory::startTelescope(): Telescope started successfully"))
                    .doOnError(e -> logger.error("Observatory::startTelescope(): Error starting telescope", e));
    }

    /**
     * Starts the dome and sets it up ready for use.
     * 
     * @return A {@link Mono} that will complete when the dome has received the command.
     */
    public Mono<Boolean> startDome() {
        // If the dome is not connected, connect it
        Mono<Boolean> ret = dome.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : dome.connect()
                                    );

        // Make sure the dome is not slaved
        ret = ret.then(this.setSlaved(false));

        // If the dome is parked and can unpark, unpark it
        ret = ret.then(
                dome.getCapabilities()
                    .zipWith(dome.isParked().onErrorReturn(false))
                    .flatMap(tuples -> {
                        boolean shouldUnpark = tuples.getT1().canUnpark() && tuples.getT2();
                        return shouldUnpark ? dome.unpark() : Mono.just(true);
                    }));

        // If the dome can find home and is not at home, find home to make sure it's calibrated
        ret = ret.then(
                dome.getCapabilities()
                    .zipWith(dome.isAtHome().onErrorReturn(true))
                    .flatMap(tuples -> {
                        boolean canFindHome = tuples.getT1().canFindHome();
                        boolean isAtHome = tuples.getT2();
                        return canFindHome && !isAtHome ? dome.findHome() : Mono.just(true);
                    }));

        return ret.doOnSuccess(s -> logger.info("Observatory::startDome(): Dome started successfully"))
                    .doOnError(e -> logger.error("Observatory::startDome(): Error starting dome", e));
    }

    /**
     * Starts the camera and sets it up ready for use.
     * 
     * @return A {@link Mono} that will complete when the camera has received the command.
     */
    public Mono<Boolean> startCamera() {
        Mono<Boolean> ret = camera.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : camera.connect()
                                    );
        
        // If the camera has a cooler, turn it on and cool the sensor down
        ret = ret.then(
                camera.getCapabilities()
                    .flatMap(capabilities -> capabilities.canSetCoolerTemp()
                        ? camera.setCooler(true).then(camera.cooldown())
                        : Mono.just(true)
                    ));
        
        return ret.doOnSuccess(s -> logger.info("Observatory::startCamera(): Camera started successfully"))
                    .doOnError(e -> logger.error("Observatory::startCamera(): Error starting camera", e));
    }

    /**
     * Starts the observatory and sets it up ready for use.
     * It should unpark the telescope and dome, set them at their home positions,
     * open the shutter and start chilling the camera.
     * 
     * This {@link Mono} will not complete until the observatory is ready for use.
     * 
     * Note: This method will not open the dome nor slave it to the telescope.
     * 
     * @return A {@link Mono} that will complete when the observatory is ready for use.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible.
    */
    public Mono<Boolean> startAwait() throws DeviceUnavailableException {
        return connectAll().then(
                    Mono.whenDelayError(
                        startTelescopeAwait(),
                        startDomeAwait(),
                        startCamera())
                    .thenReturn(true)
                )
                .doOnSuccess(s -> logger.info("Observatory::startAwaitAwait(): Observatory started successfully"))
                .doOnError(e -> logger.error("Observatory::startAwaitAwait(): Error starting observatory", e));
    }

    /**
     * Starts the telescope synchronously and sets it up ready for use.
     * 
     * @return A {@link Mono} that will complete when the telescope has completed the command.
     */
    public Mono<Boolean> startTelescopeAwait() {
        // If the telescope is not connected, connect it
        Mono<Boolean> ret = telescope.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : telescope.connect()
                                    );
        
        // If the telescope is parked and can unpark, unpark it
        ret = ret.then(
                telescope.getCapabilities()
                    .zipWith(telescope.isParked().onErrorReturn(false))
                    .flatMap(tuples -> {
                        boolean shouldUnpark = tuples.getT1().canUnpark() && tuples.getT2();
                        return shouldUnpark ? telescope.unparkAwait() : Mono.just(true);
                    }));
        
        // If the telescope can track, disable tracking
        ret = ret.then(
                telescope.getCapabilities()
                    .zipWith(telescope.isTracking().onErrorReturn(false))
                    .flatMap(tuples -> {
                        boolean canTrack = tuples.getT1().canTrack();
                        boolean isTracking = tuples.getT2();
                        return canTrack && isTracking ? telescope.setTracking(false) : Mono.just(true);
                    }));
                    
        // If the telescope can find home and is not at home, find home
        ret = ret.then(
                telescope.getCapabilities()
                    .zipWith(telescope.isAtHome().onErrorReturn(true))
                    .flatMap(tuples -> {
                        boolean canFindHome = tuples.getT1().canFindHome();
                        boolean isAtHome = tuples.getT2();
                        return canFindHome && !isAtHome ? telescope.findHomeAwait() : Mono.just(true);
                    }));

        return ret.doOnSuccess(s -> logger.info("Observatory::startTelescopeAwait(): Telescope started successfully"))
                    .doOnError(e -> logger.error("Observatory::startTelescopeAwait(): Error starting telescope", e));
    }

    /**
     * Starts the dome synchronously and sets it up ready for use.
     * 
     * @return A {@link Mono} that will complete when the dome has completed the command.
     */
    public Mono<Boolean> startDomeAwait() {
        // If the dome is not connected, connect it
        Mono<Boolean> ret = dome.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : dome.connect()
                                    );

        // Make sure the dome is not slaved
        ret = ret.then(this.setSlaved(false));

        // If the dome is parked and can unpark, unpark it
        ret = ret.then(
                dome.getCapabilities()
                    .zipWith(dome.isParked().onErrorReturn(false))
                    .flatMap(tuples -> {
                        boolean shouldUnpark = tuples.getT1().canUnpark() && tuples.getT2();
                        return shouldUnpark ? dome.unparkAwait() : Mono.just(true);
                    }));

        // If the dome can find home and is not at home, find home to make sure it's calibrated
        ret = ret.then(
                dome.getCapabilities()
                    .zipWith(dome.isAtHome().onErrorReturn(true))
                    .flatMap(tuples -> {
                        boolean canFindHome = tuples.getT1().canFindHome();
                        boolean isAtHome = tuples.getT2();
                        return canFindHome && !isAtHome ? dome.findHomeAwait() : Mono.just(true);
                    }));

        return ret.doOnSuccess(s -> logger.info("Observatory::startDomeAwait(): Dome started successfully"))
                    .doOnError(e -> logger.error("Observatory::startDomeAwait(): Error starting dome", e));
    }

    /**
     * Stops the observatory and puts it into a safe state.
     * It should close the shutter, park the telescope and dome, and turn off
     * the camera cooler.
     * 
     * Note: This method will not disconnect the devices.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     * 
     * @throws DeviceUnavailableException If a device is unaccessible.
     */
    public Mono<Boolean> stop() throws DeviceUnavailableException {
        return connectAll().then(
                    Mono.whenDelayError(
                        stopTelescope(),
                        stopDome(),
                        stopCamera())
                    .thenReturn(true)
                )
                .doOnSuccess(s -> logger.info("Observatory::stop(): Observatory stopped successfully"))
                .doOnError(e -> logger.error("Observatory::stop(): Error stopping observatory", e));
    }

    /**
     * Stops the telescope synchronously and puts it into a safe state.
     * <p>
     * Note: This method will not disconnect the telescope.
     * 
     * @return A {@link Mono} that will complete when the telescope has completed the command.
     */
    public Mono<Boolean> stopTelescope() {
        return telescope.isConnected().flatMap(connected -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(connected))
                return Mono.just(true);

            // If the telescope is tracking and can track, disable tracking
            Mono<Boolean> ret = telescope.getCapabilities()
                                    .zipWith(telescope.isTracking().onErrorReturn(false))
                                    .flatMap(tuples -> {
                                        boolean canTrack = tuples.getT1().canTrack();
                                        boolean isTracking = tuples.getT2();
                                        return canTrack && isTracking
                                            ? telescope.setTracking(false)
                                            : Mono.just(true);
                                    });

            // If the telescope is slewing and can slew, abort slew
            ret = ret.then(
                    telescope.getCapabilities()
                        .zipWith(telescope.isSlewing().onErrorReturn(false))
                        .flatMap(tuples -> {
                            boolean canSlew = tuples.getT1().canSlew();
                            boolean isSlewing = tuples.getT2();
                            return canSlew && isSlewing
                                ? telescope.abortSlew()
                                : Mono.just(true);
                        }));

            // If the telescope is not parked and can park, park it
            ret = ret.then(
                    telescope.getCapabilities()
                        .zipWith(telescope.isParked().onErrorReturn(true))
                        .flatMap(tuples -> {
                            boolean canPark = tuples.getT1().canPark();
                            boolean isParked = tuples.getT2();
                            return canPark && !isParked
                                ? telescope.park()
                                : Mono.just(true);
                        }));
            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::stopTelescope(): Telescope stopped successfully"))
        .doOnError(e -> logger.error("Observatory::stopTelescope(): Error stopping telescope", e));
    }

    /**
     * Stops the dome synchronously and puts it into a safe state.
     * <p>
     * Note: This method will not disconnect the dome.
     * 
     * @return A {@link Mono} that will complete when the dome has completed the command.
     */
    public Mono<Boolean> stopDome() {
        return dome.isConnected().flatMap(connected -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(connected))
                return Mono.just(true);

            // Disable slaving and stop slewing
            Mono<Boolean> ret = this.setSlaved(false)
                                    .then(dome.halt());

            // If the dome has shutter control and the shutter is not closed, close it
            ret = ret.then(
                    dome.getCapabilities()
                        .zipWith(dome.isShutterClosed().onErrorReturn(true))
                        .flatMap(tuples -> {
                            boolean canControlShutter = tuples.getT1().canShutter();
                            boolean isShutterClosed = tuples.getT2();
                            return canControlShutter && !isShutterClosed
                                ? dome.closeShutter()
                                : Mono.just(true);
                        }));

            // If the dome is not parked and can park, park it
            ret = ret.then(
                    dome.getCapabilities()
                        .zipWith(dome.isParked().onErrorReturn(true))
                        .flatMap(tuples -> {
                            boolean canPark = tuples.getT1().canPark();
                            boolean isParked = tuples.getT2();
                            return canPark && !isParked
                                ? dome.park()
                                : Mono.just(true);
                        }));
            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::stopDome(): Dome stopped successfully"))
        .doOnError(e -> logger.error("Observatory::stopDome(): Error stopping dome", e));
    }


    /**
     * Stops the camera synchronously and puts it into a safe state.
     * <p>
     * Note: This method will not disconnect the camera.
     * 
     * @return A {@link Mono} that will complete when the camera has completed the command.
     */
    public Mono<Boolean> stopCamera() {
        return camera.isConnected().zipWith(camera.getCapabilities()).flatMap(tuples -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(tuples.getT1()))
                return Mono.just(true);

            boolean canSetCoolerTemp = tuples.getT2().canSetCoolerTemp();

            Mono<Boolean> ret = Mono.just(true);

            // If the camera is being cooled, warm it up
            if (canSetCoolerTemp) {
                ret = camera.isCoolerOn().flatMap(coolOn -> Boolean.TRUE.equals(coolOn)
                                ? camera.warmup()
                                : Mono.just(true));
            }

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::stopCamera(): Camera stopped successfully"))
        .doOnError(e -> logger.error("Observatory::stopCamera(): Error stopping camera", e));
    }

    /**
     * Stops the observatory and puts it into a safe state.
     * It should close the shutter, park the telescope and dome, warmup the
     * camera and turn off the camera cooler.
     * 
     * Note: This method will not disconnect the devices.
     * 
     * @return A {@link Mono} that will complete when the observatory is in a safe state. 
     */
    public Mono<Boolean> stopAwait() {
        return connectAll().then(
                    Mono.whenDelayError(
                        stopTelescopeAwait(),
                        stopDomeAwait(),
                        stopCameraAwait())
                    .thenReturn(true)
                )
                .doOnSuccess(s -> logger.info("Observatory::stopAwait(): Observatory stopped successfully"))
                .doOnError(e -> logger.error("Observatory::stopAwait(): Error stopping observatory", e));
    }


    /**
     * Stops the dome synchronously and puts it into a safe state.
     * <p>
     * Note: This method will not disconnect the telescope.
     * 
     * @return A {@link Mono} that will complete when the dome has completed the command.
     */
    public Mono<Boolean> stopTelescopeAwait() {
        return telescope.isConnected().flatMap(connected -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(connected))
                return Mono.just(true);

            // If the telescope is tracking and can track, disable tracking
            Mono<Boolean> ret = telescope.getCapabilities()
                                    .zipWith(telescope.isTracking().onErrorReturn(false))
                                    .flatMap(tuples -> {
                                        boolean canTrack = tuples.getT1().canTrack();
                                        boolean isTracking = tuples.getT2();
                                        return canTrack && isTracking
                                            ? telescope.setTracking(false)
                                            : Mono.just(true);
                                    });

            // If the telescope is slewing and can slew, abort slew
            ret = ret.then(
                    telescope.getCapabilities()
                        .zipWith(telescope.isSlewing().onErrorReturn(false))
                        .flatMap(tuples -> {
                            boolean canSlew = tuples.getT1().canSlew();
                            boolean isSlewing = tuples.getT2();
                            return canSlew && isSlewing
                                ? telescope.abortSlew()
                                : Mono.just(true);
                        }));

            // If the telescope is not parked and can park, park it
            ret = ret.then(
                    telescope.getCapabilities()
                        .zipWith(telescope.isParked().onErrorReturn(true))
                        .flatMap(tuples -> {
                            boolean canPark = tuples.getT1().canPark();
                            boolean isParked = tuples.getT2();
                            return canPark && !isParked
                                ? telescope.parkAwait()
                                : Mono.just(true);
                        }));

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::stopTelescopeAwait(): Telescope stopped successfully"))
        .doOnError(e -> logger.error("Observatory::stopTelescopeAwait(): Error stopping telescope", e));
    }

    /**
     * Stops the dome synchronously and puts it into a safe state.
     * <p>
     * Note: This method will not disconnect the dome.
     * 
     * @return A {@link Mono} that will complete when the dome has completed the command.
     */
    public Mono<Boolean> stopDomeAwait() {
        return dome.isConnected().flatMap(connected -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(connected))
                return Mono.just(true);

            // Disable slaving and stop slewing
            Mono<Boolean> ret = this.setSlavedAwait(false)
                                    .then(dome.halt());

            // If the dome has shutter control and the shutter is not closed, close it
            Mono<Boolean> thread1 = dome.getCapabilities()
                                        .zipWith(dome.isShutterClosed().onErrorReturn(true))
                                        .flatMap(tuples -> {
                                            boolean canControlShutter = tuples.getT1().canShutter();
                                            boolean isShutterClosed = tuples.getT2();
                                            return canControlShutter && !isShutterClosed
                                                ? dome.closeShutterAwait()
                                                : Mono.just(true);
                                        });

            // If the dome is not parked and can park, park it
            Mono<Boolean> thread2 = dome.getCapabilities()
                                        .zipWith(dome.isParked().onErrorReturn(true))
                                        .flatMap(tuples -> {
                                            boolean canPark = tuples.getT1().canPark();
                                            boolean isParked = tuples.getT2();
                                            return canPark && !isParked
                                                ? dome.parkAwait()
                                                : Mono.just(true);
                                        });

            // Do both in parallel and return when both are done
            ret = ret.then(Mono.whenDelayError(thread1,thread2).thenReturn(true));
                    
            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::stopDomeAwait(): Dome stopped successfully"))
        .doOnError(e -> logger.error("Observatory::stopDomeAwait(): Error stopping dome", e));
    }

    /**
     * Stops the camera synchronously and puts it into a safe state.
     * <p>
     * Note: This method will not disconnect the camera.
     * 
     * @return A {@link Mono} that will complete when the camera has completed the command.
     */
    public Mono<Boolean> stopCameraAwait() {
        return camera.isConnected().zipWith(camera.getCapabilities()).flatMap(tuples -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(tuples.getT1()))
                return Mono.just(true);

            boolean canSetCoolerTemp = tuples.getT2().canSetCoolerTemp();

            Mono<Boolean> ret = Mono.just(true);

            // If the camera is being cooled, warm it up
            if (canSetCoolerTemp) {
                ret = camera.isCoolerOn().flatMap(coolOn -> Boolean.TRUE.equals(coolOn)
                                ? camera.warmupAwait().then(camera.setCooler(false))
                                : Mono.just(true));
            }

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::stopCameraAwait(): Camera stopped successfully"))
        .doOnError(e -> logger.error("Observatory::stopCameraAwait(): Error stopping camera", e));
    }

    /**
     * Aborts all operations in progress and puts the observatory into an idle.
     * 
     * Note: This method will not disconnect the devices nor will close the shutter.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     */
    public Mono<Boolean> abort() {
        return connectAll().then(
                    Mono.whenDelayError(
                        abortTelescope(),
                        abortDome()
                    ).thenReturn(true)
                )
                .doOnSuccess(s -> logger.info("Observatory::abort(): Observatory aborted successfully"))
                .doOnError(e -> logger.error("Observatory::abort(): Error aborting observatory", e));
    }

    /**
     * Aborts the telescope's operations.
     * @return A {@link Mono} that will complete when the telescope has received the command.
     */
    public Mono<Boolean> abortTelescope() {
        return telescope.isConnected().flatMap(connected -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(connected))
                return Mono.just(true);

            // If the telescope is tracking and can track, disable tracking
            Mono<Boolean> ret = telescope.getCapabilities()
                                    .zipWith(telescope.isTracking().onErrorReturn(false))
                                    .flatMap(tuples -> {
                                        boolean canTrack = tuples.getT1().canTrack();
                                        boolean isTracking = tuples.getT2();
                                        return canTrack && isTracking
                                            ? telescope.setTracking(false)
                                            : Mono.just(true);
                                    });

            // If the telescope is slewing and can slew, abort slew
            ret = ret.then(
                    telescope.getCapabilities()
                        .zipWith(telescope.isSlewing().onErrorReturn(false))
                        .flatMap(tuples -> {
                            boolean canSlew = tuples.getT1().canSlew();
                            boolean isSlewing = tuples.getT2();
                            return canSlew && isSlewing
                                ? telescope.abortSlew()
                                : Mono.just(true);
                        }));

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::abortTelescope(): Telescope aborted successfully"))
        .doOnError(e -> logger.error("Observatory::abortTelescope(): Error aborting telescope", e));
    }

    /**
     * Aborts the dome's operations.
     * @return A {@link Mono} that will complete when the dome has received the command.
     */
    private Mono<Boolean> abortDome() {
        return dome.isConnected().flatMap(connected -> {
            // If already disconnected, do nothing and return
            if (Boolean.FALSE.equals(connected)) {
                return Mono.just(true);
            } else {
                // Disable slaving and stop slewing
                return this.setSlaved(false).then(dome.halt());
            }
        })
        .doOnSuccess(s -> logger.info("Observatory::abortDome(): Dome aborted successfully"))
        .doOnError(e -> logger.error("Observatory::abortDome(): Error aborting dome", e));
    }


    /**
     * Slew the telescope and dome asynchronously to the given coordinates.
     * 
     * @param ra The right ascension in hours.
     * @param dec The declination in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices receive the
     *         signal.
     */
    public Mono<Boolean> slewTogetherRaDec(double ra, double dec) {
        Mono<double[]> asAltAz = ephemeridesSolver.raDecToAltAz(ra, dec);

        return connectAll().then(asAltAz.zipWith(this.isSlaved())).flatMap(tuples -> {
            double[] altAz = tuples.getT1();
            boolean slaved = Boolean.TRUE.equals(tuples.getT2());

            Mono<Boolean> ret = Mono.whenDelayError(
                                    slewTelescopeRaDec(ra, dec),
                                    slewDome(altAz[0], altAz[1])
                                ).thenReturn(true);
            
            // Restore slaving
            if (slaved) {
                ret = ret.then(this.setSlaved(true));
            }

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::slewTogetherRaDec(ra={},dec={}): Telescope and dome slewed successfully", ra, dec))
        .doOnError(e -> logger.error("Observatory::slewTogetherRaDec(): Error slewing telescope and dome", e));
    }

    /**
     * Slew the telescope and dome asynchronously to the given coordinates.
     * 
     * @param alt The altitude in hours.
     * @param az The azimuth in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices receive the
     *         signal.
     */
    public Mono<Boolean> slewTogetherAltAz(double alt, double az) {
        return connectAll().then(this.isSlaved()).flatMap(slaved -> {
            Mono<Boolean> ret = Mono.whenDelayError(
                                    slewTelescopeAltAz(alt, az),
                                    slewDome(alt, az)
                                ).thenReturn(true);
            
            // Restore slaving
            if (Boolean.TRUE.equals(slaved)) {
                ret = ret.then(this.setSlaved(true));
            }

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::slewTogetherAltAz(alt={},az={}): Telescope and dome slewed successfully", alt, az))
        .doOnError(e -> logger.error("Observatory::slewTogetherAltAz(): Error slewing telescope and dome", e));
    }

    /**
     * Slew the telescope and dome asynchronously to the given coordinates.
     * 
     * @param ra The right ascension in hours.
     * @param dec The declination in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices receive the
     *         signal.
     */
    @SuppressWarnings("java:S3776")
    public Mono<Boolean> slewTelescopeRaDec(double ra, double dec) {
        // If the telescope is not connected, connect it
        Mono<Boolean> ret = telescope.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : telescope.connect()
                                    );

        ret = ret.then(telescope.isParked().flatMap(parked -> {
            boolean wasParked = Boolean.TRUE.equals(parked);

            // If the telescope is parked and can unpark, unpark it
            Mono<Boolean> slew = telescope.getCapabilities()
                                            .flatMap(caps -> caps.canUnpark() && wasParked
                                                ? telescope.unpark()
                                                : Mono.just(true)
                                            );

            // If the telescope can track, disable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .zipWith(telescope.isTracking().onErrorReturn(false))
                            .flatMap(tuples -> {
                                boolean canTrack = tuples.getT1().canTrack();
                                boolean isTracking = tuples.getT2();
                                return canTrack && isTracking
                                    ? telescope.setTracking(false)
                                    : Mono.just(true);
                            }));

            // Find home if the telescope just unparked and is not at home,
            // for calibration purposes
            if (wasParked) {
                slew = slew.then(
                            telescope.getCapabilities()
                                .zipWith(telescope.isAtHome().onErrorReturn(true))
                                .flatMap(tuples -> {
                                    boolean canFindHome = tuples.getT1().canFindHome();
                                    boolean isAtHome = tuples.getT2();
                                    return canFindHome && !isAtHome
                                        ? telescope.findHome()
                                        : Mono.just(true);
                                })
                            );
            }
            
            // If the telescope can track, enable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canTrack()
                                ? telescope.setTracking(false)
                                : Mono.just(true)
                            )
                        );

            // If the telescope can slew, slew it
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canSlew()
                                ? telescope.slewToCoords(ra, dec)
                                : Mono.just(true)
                            )
                        );
            return slew;
        }));

        return ret.doOnSuccess(s -> logger.info("Observatory::slewTelescopeRaDec(ra={},dec={}): Telescope slewed successfully", ra, dec))
                    .doOnError(e -> logger.error("Observatory::slewTelescopeRaDec(): Error slewing telescope", e));
    }

    /**
     * Slew the telescope asynchronously to the given coordinates.
     * 
     * @param alt The altitude in degrees.
     * @param az The azimuth in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices receive the
     *         signal.
     */
    @SuppressWarnings("java:S3776")
    public Mono<Boolean> slewTelescopeAltAz(double alt, double az) {
        // If the telescope is not connected, connect it
        Mono<Boolean> ret = telescope.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : telescope.connect()
                                    );

        ret = ret.then(telescope.isParked().flatMap(parked -> {
            boolean wasParked = Boolean.TRUE.equals(parked);

            // If the telescope is parked and can unpark, unpark it
            Mono<Boolean> slew = telescope.getCapabilities()
                                            .flatMap(caps -> caps.canUnpark() && wasParked
                                                ? telescope.unpark()
                                                : Mono.just(true)
                                            );

            // If the telescope can track, disable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .zipWith(telescope.isTracking().onErrorReturn(false))
                            .flatMap(tuples -> {
                                boolean canTrack = tuples.getT1().canTrack();
                                boolean isTracking = tuples.getT2();
                                return canTrack && isTracking
                                    ? telescope.setTracking(false)
                                    : Mono.just(true);
                            }));

            // Find home if the telescope just unparked and is not at home,
            // for calibration purposes
            if (wasParked) {
                slew = slew.then(
                            telescope.getCapabilities()
                                .zipWith(telescope.isAtHome().onErrorReturn(true))
                                .flatMap(tuples -> {
                                    boolean canFindHome = tuples.getT1().canFindHome();
                                    boolean isAtHome = tuples.getT2();
                                    return canFindHome && !isAtHome
                                        ? telescope.findHome()
                                        : Mono.just(true);
                                })
                            );
            }

            // If the telescope can slew, slew it
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canSlew()
                                ? telescope.slewToAltAz(alt, az)
                                : Mono.just(true)
                            )
                        );
            
            // If the telescope can track, enable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canTrack()
                                ? telescope.setTracking(true)
                                : Mono.just(true)
                            )
                        );
            return slew;          
        }));

        return ret.doOnSuccess(s -> logger.info("Observatory::slewTelescopeAltAz(alt={},az={}): Telescope slewed successfully", alt, az))
                    .doOnError(e -> logger.error("Observatory::slewTelescopeAltAz(): Error slewing telescope", e));
    }

    /**
     * Slew the dome asynchronously to the given coordinates.
     * 
     * @param alt The altitude in degrees.
     * @param az The azimuth in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices receive the
     *         signal.
     */
    public Mono<Boolean> slewDome(double alt, double az) {
        // If the dome is not connected, connect it
        Mono<Boolean> ret = dome.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : dome.connect()
                                    );

        // Disable slaving
        ret = ret.then(this.setSlaved(false));

        // If the dome is parked and can unpark, unpark it and find home to calibrate
        ret = ret.then(dome.getCapabilities()
                                .zipWith(dome.isParked().onErrorReturn(false))
                                .flatMap(tuples -> {
                                    boolean canUnpark = tuples.getT1().canUnpark();
                                    boolean isParked = tuples.getT2();
                                    return canUnpark && isParked
                                        ? dome.unpark().then(dome.findHome())
                                        : Mono.just(true);
                                })
                        );
                        
        // If the dome has shutter control, open the shutter
        Mono<Boolean> thread1 = dome.getCapabilities()
                                    .zipWith(dome.isShutterOpen().onErrorReturn(true))
                                    .flatMap(tuples -> {
                                        boolean canShutter = tuples.getT1().canShutter();
                                        boolean isShutterOpen = tuples.getT2();
                                        return canShutter && !isShutterOpen
                                            ? dome.openShutter()
                                            : Mono.just(true);
                                    });
        
        // If the dome has altitude control, slew to the altitude
        thread1 = thread1.then(dome.getCapabilities()
                                    .flatMap(caps -> caps.canSetAltitude()
                                        ? dome.setAlt(alt)
                                        : Mono.just(true)
                                    )
                                );

        // If the dome can slew, slew to the azimuth
        Mono<Boolean> thread2 = dome.getCapabilities()
                                    .flatMap(caps -> caps.canSetAzimuth()
                                        ? dome.slew(az)
                                        : Mono.just(true)
                                    );

        return ret.then(Mono.whenDelayError(thread1, thread2).thenReturn(true))
                .doOnSuccess(s -> logger.info("Observatory::slewDome(alt={},az={}): Dome slewed successfully", alt, az))
                .doOnError(e -> logger.error("Observatory::slewDome(): Error slewing dome", e));
    }






    /**
     * Slew the telescope and dome synchronously to the given coordinates.
     * 
     * @param ra The right ascension in hours.
     * @param dec The declination in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices end the slew.
     */
    public Mono<Boolean> slewTogetherRaDecAwait(double ra, double dec) {
        Mono<double[]> asAltAz = ephemeridesSolver.raDecToAltAz(ra, dec);

        return connectAll().then(asAltAz.zipWith(this.isSlaved())).flatMap(tuples -> {
            double[] altAz = tuples.getT1();
            boolean slaved = Boolean.TRUE.equals(tuples.getT2());

            Mono<Boolean> ret = Mono.whenDelayError(
                                    slewTelescopeRaDecAwait(ra, dec),
                                    slewDomeAwait(altAz[0], altAz[1])
                                ).thenReturn(true);
            
            // Restore slaving
            if (slaved) {
                ret = ret.then(this.setSlaved(true));
            }

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::slewTogetherRaDecAwait(ra={},dec={}): Telescope and dome slewed successfully", ra, dec))
        .doOnError(e -> logger.error("Observatory::slewTogetherRaDecAwait(): Error slewing telescope and dome", e));
    }

    /**
     * Slew the telescope and dome synchronously to the given coordinates.
     * 
     * @param alt The altitude in hours.
     * @param az The azimuth in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices end the slew.
     */
    public Mono<Boolean> slewTogetherAltAzAwait(double alt, double az) {
        return connectAll().then(this.isSlaved()).flatMap(slaved -> {
            Mono<Boolean> ret = Mono.whenDelayError(
                                    slewTelescopeAltAzAwait(alt, az),
                                    slewDomeAwait(alt, az)
                                ).thenReturn(true);
            
            // Restore slaving
            if (Boolean.TRUE.equals(slaved)) {
                ret = ret.then(this.setSlaved(true));
            }

            return ret;
        })
        .doOnSuccess(s -> logger.info("Observatory::slewTogetherAltAzAwait(alt={},az={}): Telescope and dome slewed successfully", alt, az))
        .doOnError(e -> logger.error("Observatory::slewTogetherAltAzAwait(): Error slewing telescope and dome", e));
    }

    /**
     * Slew the telescope and dome synchronously to the given coordinates.
     * 
     * @param ra The right ascension in hours.
     * @param dec The declination in degrees.
     * 
     * @return A {@link Mono} that will complete when the devices complete the
     *          operation.
     */
    @SuppressWarnings("java:S3776")
    public Mono<Boolean> slewTelescopeRaDecAwait(double ra, double dec) {
        // If the telescope is not connected, connect it
        Mono<Boolean> ret = telescope.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : telescope.connect()
                                    );

        ret = ret.then(telescope.isParked().flatMap(parked -> {
            boolean wasParked = Boolean.TRUE.equals(parked);

            // If the telescope is parked and can unpark, unpark it
            Mono<Boolean> slew = telescope.getCapabilities()
                                            .flatMap(caps -> caps.canUnpark() && wasParked
                                                ? telescope.unparkAwait()
                                                : Mono.just(true)
                                            );

            // If the telescope can track, disable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .zipWith(telescope.isTracking().onErrorReturn(false))
                            .flatMap(tuples -> {
                                boolean canTrack = tuples.getT1().canTrack();
                                boolean isTracking = tuples.getT2();
                                return canTrack && isTracking
                                    ? telescope.setTracking(false)
                                    : Mono.just(true);
                            }));

            // Find home if the telescope just unparked and is not at home,
            // for calibration purposes
            if (wasParked) {
                slew = slew.then(
                            telescope.getCapabilities()
                                .zipWith(telescope.isAtHome().onErrorReturn(true))
                                .flatMap(tuples -> {
                                    boolean canFindHome = tuples.getT1().canFindHome();
                                    boolean isAtHome = tuples.getT2();
                                    return canFindHome && !isAtHome
                                        ? telescope.findHomeAwait()
                                        : Mono.just(true);
                                })
                            );
            }
            
            // If the telescope can track, enable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canTrack()
                                ? telescope.setTracking(false)
                                : Mono.just(true)
                            )
                        );

            // If the telescope can slew, slew it
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canSlew()
                                ? telescope.slewToCoordsAwait(ra, dec)
                                : Mono.just(true)
                            )
                        );
            return slew;
        }));

        return ret.doOnSuccess(s -> logger.info("Observatory::slewTelescopeRaDecAwait(ra={},dec={}): Telescope slewed successfully", ra, dec))
                    .doOnError(e -> logger.error("Observatory::slewTelescopeRaDecAwait(): Error slewing telescope", e));
    }

    /**
     * Slew the telescope synchronously to the given coordinates.
     * 
     * @param alt The altitude in degrees.
     * @param az The azimuth in degrees.
     * 
     * @return A {@link Mono} that will complete when the device completes the
     *          operation.
     */
    @SuppressWarnings("java:S3776")
    public Mono<Boolean> slewTelescopeAltAzAwait(double alt, double az) {
        // If the telescope is not connected, connect it
        Mono<Boolean> ret = telescope.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : telescope.connect()
                                    );

        ret = ret.then(telescope.isParked().flatMap(parked -> {
            boolean wasParked = Boolean.TRUE.equals(parked);

            // If the telescope is parked and can unpark, unpark it
            Mono<Boolean> slew = telescope.getCapabilities()
                                            .flatMap(caps -> caps.canUnpark() && wasParked
                                                ? telescope.unparkAwait()
                                                : Mono.just(true)
                                            );

            // If the telescope can track, disable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .zipWith(telescope.isTracking().onErrorReturn(false))
                            .flatMap(tuples -> {
                                boolean canTrack = tuples.getT1().canTrack();
                                boolean isTracking = tuples.getT2();
                                return canTrack && isTracking
                                    ? telescope.setTracking(false)
                                    : Mono.just(true);
                            }));

            // Find home if the telescope just unparked and is not at home,
            // for calibration purposes
            if (wasParked) {
                slew = slew.then(
                            telescope.getCapabilities()
                                .zipWith(telescope.isAtHome().onErrorReturn(true))
                                .flatMap(tuples -> {
                                    boolean canFindHome = tuples.getT1().canFindHome();
                                    boolean isAtHome = tuples.getT2();
                                    return canFindHome && !isAtHome
                                        ? telescope.findHomeAwait()
                                        : Mono.just(true);
                                })
                            );
            }

            // If the telescope can slew, slew it
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canSlew()
                                ? telescope.slewToAltAzAwait(alt, az)
                                : Mono.just(true)
                            )
                        );
            
            // If the telescope can track, enable tracking
            slew = slew.then(
                        telescope.getCapabilities()
                            .flatMap(caps -> caps.canTrack()
                                ? telescope.setTracking(true)
                                : Mono.just(true)
                            )
                        );
            return slew;          
        }));

        return ret.doOnSuccess(s -> logger.info("Observatory::slewTelescopeAltAz(alt={},az={}): Telescope slewed successfully", alt, az))
                    .doOnError(e -> logger.error("Observatory::slewTelescopeAltAz(): Error slewing telescope", e));
    }

    /**
     * Slew the dome synchronously to the given coordinates.
     * 
     * @param alt The altitude in degrees.
     * @param az The azimuth in degrees.
     * 
     * @return A {@link Mono} that will complete when the device completes the
     *          operation.
     */
    public Mono<Boolean> slewDomeAwait(double alt, double az) {
        // If the dome is not connected, connect it
        Mono<Boolean> ret = dome.isConnected()
                                    .flatMap(connected -> Boolean.TRUE.equals(connected)
                                        ? Mono.just(true)
                                        : dome.connect()
                                    );

        // Disable slaving
        ret = ret.then(this.setSlaved(false));

        // If the dome is parked and can unpark, unpark it and find home to calibrate
        ret = ret.then(dome.getCapabilities()
                                .zipWith(dome.isParked().onErrorReturn(false))
                                .flatMap(tuples -> {
                                    boolean canUnpark = tuples.getT1().canUnpark();
                                    boolean isParked = tuples.getT2();
                                    return canUnpark && isParked
                                        ? dome.unparkAwait().then(dome.findHomeAwait())
                                        : Mono.just(true);
                                })
                        );
                        
        // If the dome has shutter control, open the shutter
        Mono<Boolean> thread1 = dome.getCapabilities()
                                    .zipWith(dome.isShutterOpen().onErrorReturn(true))
                                    .flatMap(tuples -> {
                                        boolean canShutter = tuples.getT1().canShutter();
                                        boolean isShutterOpen = tuples.getT2();
                                        return canShutter && !isShutterOpen
                                            ? dome.openShutterAwait()
                                            : Mono.just(true);
                                    });
        
        // If the dome has altitude control, slew to the altitude
        thread1 = thread1.then(dome.getCapabilities()
                                    .flatMap(caps -> caps.canSetAltitude()
                                        ? dome.setAltAwait(alt)
                                        : Mono.just(true)
                                    )
                                );

        // If the dome can slew, slew to the azimuth
        Mono<Boolean> thread2 = dome.getCapabilities()
                                    .flatMap(caps -> caps.canSetAzimuth()
                                        ? dome.slewAwait(az)
                                        : Mono.just(true)
                                    );

        return ret.then(Mono.whenDelayError(thread1, thread2).thenReturn(true))
                .doOnSuccess(s -> logger.info("Observatory::slewDomeAwait(alt={},az={}): Dome slewed successfully", alt, az))
                .doOnError(e -> logger.error("Observatory::slewDomeAwait(): Error slewing dome", e));
    }


    //#region Image processing

    /**
     * Starts an exposure with the given parameters.
     * 
     * @param params The exposure parameters.
     * @return A {@link Mono} that will complete when the exposure has started. 
     */
    public Mono<Boolean> startExposure(ExposureParams params) {
        // Focuser starts moving to the base focus, if any
        Integer baseFocus = params.getProgram().getTarget().getBaseFocus();

        Mono<Boolean> focuserActionStart = baseFocus == null
                                    ? Mono.just(true)
                                    : focuser.moveAwait(baseFocus);
        
        // Get the filter data
        String filterName = params.getFilter();
        Mono<Tuple2<Integer, Integer>> filterData = filterName == null
                                    ? Mono.just(Tuples.of(-1, 0))
                                    : filterWheel.getFilterNames()
                                        .flatMap(names -> {
                                            int index = IntStream.range(0, names.size())
                                                                .filter(i -> names.get(i).equalsIgnoreCase(filterName))
                                                                .findFirst()
                                                                .orElse(-1);
                                            if (index < 0)
                                                return Mono.just(Tuples.of(-1, 0));
                                            return filterWheel.getFocusOffsets().map(offsets -> Tuples.of(index, offsets.get(index)));
                                        });

        // Sets up the camera
        double duration = params.getExposureTime(); 
        boolean lightFrame = params.isLightFrame();
        int binX = params.getBinX();
        int binY = params.getBinY();
        
        Mono<Boolean> cameraAction;

        if (params.usesSubFrame()) {
            int[] subFrame = new int[] {
                    params.getSubFrameX(),
                    params.getSubFrameY(),
                    params.getSubFrameWidth(),
                    params.getSubFrameHeight()
                };

            cameraAction = camera.startExposure(
                            duration,
                            lightFrame,
                            subFrame,
                            binX,
                            binY);
        } else {
            cameraAction = camera.getCapabilities().flatMap(caps -> {
                // TODO: This is a hack. The camera simulator doesn't like it when the subframe is the same size as the sensor
                int[] subFrame = new int[] {
                        5,
                        5,
                        caps.sensorX()-10,
                        caps.sensorY()-10
                    };
                return camera
                        .startExposure(
                            duration,
                            lightFrame,
                            subFrame,
                            binX,
                            binY
                        );
            });
        }

        // Redeclaration of cameraAction bc it must be effectively final
        Mono<Boolean> cameraActionFinal = cameraAction;

        return filterData.flatMap(data -> {
            int filterIndex = data.getT1();
            int focusOffset = data.getT2();

            if (filterIndex < 0) {
                // If the filter wasn't found, throw an error
                return Mono.error(new IllegalArgumentException("Filter "+ filterName +" not found"));
            }

            // After moving to base focus, move to the offset
            Mono<Boolean> focuserAction = focuserActionStart.then(focuser.moveRelativeAwait(focusOffset));

            // Also select the filter. If filter wasn't found, throw an error
            Mono<Boolean> filterAction = filterWheel.setPositionAwait(filterIndex);

            return Mono.when(focuserAction, filterAction).then(cameraActionFinal);
        });
    }

    /**
     * Waits for the exposure to complete.
     * 
     * @param timeout The timeout for the wait.
     * @return A {@link Mono} that will complete when there is an image ready.
     */
    public Mono<Boolean> waitForExposure(Duration timeout) {
        return Flux.interval(Duration.ofMillis(config.getStatusUpdateInterval()))
                .flatMap(i -> camera.isImageReady()
                    .filter(Boolean.TRUE::equals)
                    .flatMap(ready -> Mono.just(true))
                ).next()
                .timeout(timeout);
    }

    /**
     * Gets the latest image from the camera, adding all available metadata
     * from the other devices from the observatory to its header.
     * 
     * @param target If the image is of a target, the target object to add to
     *               the header. If {@code null}, no target information will be
     *               added.
     * @param author The user that created the image. If {@code null}, no
     *                creator information will be added.
     * 
     * @return A {@link Mono} that will emit the image as a {@link ImageHDU}
     *         when it is available.
     */
    public Mono<ImageHDU> getImage(AstroObject target, AltairUser author) {
        Mono<ImageHDU> image = camera.getImage();
        Mono<ObservatoryStatus> statuses = getStatus();
        Mono<WeatherWatchCapabilities> weatherWatchCapabilities = weatherWatch.getCapabilities();
        
        Mono<String> telescopeName = telescope.getCapabilities().map(TelescopeCapabilities::name).onErrorReturn("Unknown");

        AstrometricsConfig metrics = ephemeridesSolver.getConfig();
        double siteLatitude = metrics.getSiteLatitude();
        double siteLongitude = metrics.getSiteLongitude();
        double siteElevation = metrics.getSiteElevation();

        return Mono.zip(image, statuses, telescopeName, weatherWatchCapabilities).map(tuple -> {
            ImageHDU img = tuple.getT1();
            TelescopeStatus telescopeStatus = tuple.getT2().telescope();
            DomeStatus domeStatus = tuple.getT2().dome();
            FocuserStatus focuserStatus = tuple.getT2().focuser();
            FilterWheelStatus filterWheelStatus = tuple.getT2().filterWheel();
            WeatherWatchStatus weatherStatus = tuple.getT2().weatherWatch();
            WeatherWatchCapabilities weatherCapabilities = tuple.getT4();

            String telescopeNameStr = tuple.getT3();

            if (target != null) {
                addValueIfValid(img, "OBJECT", target.getName(), "Name of the object being imaged");
                if (target.shouldHaveRaDec()) {
                    addValueIfValid(img, "OBJCTRA", target.getRa(), "Right Ascension of the object, in decimal hours");
                    addValueIfValid(img, "OBJCTDEC", target.getDec(), "Declination of the object, in decimal degrees");
                }
            }

            if (author != null) {
                addValueIfValid(img, "AUTHOR", author.getUsername(), "Name of the user that created the image");
            }

            addValueIfValid(img, "TELESCOP", telescopeNameStr, "Name of the telescope");
            addValueIfValid(img, "LATITUDE", siteLatitude, "Latitude of the observatory, in decimal degrees");
            addValueIfValid(img, "LONGITUD", siteLongitude, "Longitude of the observatory, in decimal degrees");
            addValueIfValid(img, "ELEVATIO", siteElevation, "Elevation of the observatory, in meters");

            addTelescopeValues(img, telescopeStatus, (target == null || !target.shouldHaveRaDec()));
            addDomeValues(img, domeStatus);
            addFocuserValues(img, focuserStatus);
            addFilterWheelValues(img, filterWheelStatus);
            addWeatherWatchValues(img, weatherStatus, weatherCapabilities);

           return img; 
        });    
    }

    /**
     * Saves the latest exposure made by the camera to the image store.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage() {
        return getImage(null, null).flatMap(image -> {
            try {
                return Mono.just(saveImage(image));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the latest exposure made by the camera to the image store.
     * 
     * @param target The target object to add to the image's metadata. If null,
     *               no target information will be added.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage(AstroObject target) {
        return getImage(target, null).flatMap(image -> {
            try {
                return Mono.just(saveImage(image));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the latest exposure made by the camera to the image store.
     *
     * @param target The target object to add to the image's metadata. If
     *               {@code null}, no target information will be added. 
     * @param author The user that created the image. If {@code null}, no
     *                creator information will be added.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage(AstroObject target, AltairUser author) {
        return getImage(target, author).flatMap(image -> {
            try {
                return Mono.just(saveImage(image));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the latest exposure made by the camera to the image store, using
     * the given filename.
     * 
     * @param target The target object to add to the image's metadata. If null,
     *               no target information will be added.
     * @param filename The filename to save the image as. If null, the filename
     *                 will be generated from the image's metadata.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage(AstroObject target, String filename) {
        return getImage(target, null).flatMap(image -> {
            try {
                return Mono.just(saveImage(image, filename, false));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the latest exposure made by the camera to the image store, using
     * the given filename.
     * 
     * @param target The target object to add to the image's metadata. If null,
     *               no target information will be added.
     * @param author The user that created the image. If null, no creator
     *               information will be added.
     * @param filename The filename to save the image as. If null, the filename
     *                 will be generated from the image's metadata.
     * 
     * @return A {@link Mono} that will emit the path to the saved image when
     *         it is available, or an error if there was a problem saving the
     *         image.
     */
    public Mono<Path> saveImage(AstroObject target, AltairUser author, String filename) {
        return getImage(target, author).flatMap(image -> {
            try {
                return Mono.just(saveImage(image, filename, false));
            } catch (IOException e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Saves the given image to the image store.
     * 
     * @param image The image to save.
     * 
     * @return The path to the saved image.
     * 
     * @throws IOException If there was a problem saving the image.
     */
    public Path saveImage(ImageHDU image) throws IOException {
        return saveImage(image, null, false);
    }

    /**
     * Saves the given image to the image store, using the given filename.
     * If the filename is null, the filename will be generated from the image's
     * metadata.
     * 
     * @param image The image to save.
     * @param filename The filename to save the image as. If null, the filename
     *                 will be generated from the image's metadata.
     * @param useCompression If true, the image will be compressed using gzip and
     *                       saved with a .gz extension.
     * 
     * @return The path to the saved image.
     * @throws IOException If there was an error saving the image.
     */
    @SuppressWarnings({"java:S3776", "java:S6541"})
    public Path saveImage(ImageHDU image, String filename, boolean useCompression) throws IOException {
        Path imageStore = config.getCamera().getImageStorePath();

        // Create necessary directories
        if (Files.notExists(imageStore)) {
            Files.createDirectories(imageStore);
        }

        if (filename == null) {
            String obsDate = image.getTrimmedString("DATE-OBS");
            if (obsDate == null) {
                obsDate = Instant.now().toString();
            }

            String objName = image.getTrimmedString("OBJECT");
            if (objName == null) {
                objName = "";
            }

            String filter = image.getTrimmedString("FILTER");
            if (filter == null) {
                filter = "";
            }

            String imgType = image.getTrimmedString("IMAGETYP");
            if (imgType == null) {
                imgType = "";
            } else {
                switch (imgType) {
                    case "Light Frame":
                        imgType = "";
                        break;
                    case "Dark Frame":
                        imgType = "Dark";
                        break;
                    case "Bias Frame":
                        imgType = "Bias";
                        break;
                    case "Flat Frame":
                        imgType = "Flat";
                        break;
                    case "Tricolor Image":
                        imgType = "RGB";
                        break;
                    default:
                        imgType = "";
                        break;
                }
            }

            String extension = useCompression ? ".fit.gz" : ".fit";

            filename = String.format(Locale.US,"%s_%s_%s_%s%s", obsDate, objName, filter, imgType, extension);
        } else {
            // Add extension if necessary
            if (!(filename.toUpperCase().endsWith(".FIT") || filename.toUpperCase().endsWith(".FITS"))) {
                filename += useCompression ? ".fit.gz" : ".fit";
            }
        }

        filename = filename.replaceAll("[^a-zA-Z0-9\\._\\-]", "_");

        Path filepath = imageStore.resolve(filename);

        try (
            Fits fits = new Fits();
            FitsOutputStream out = useCompression ?
                new FitsOutputStream(new GZIPOutputStream(Files.newOutputStream(filepath))) :
                new FitsOutputStream(Files.newOutputStream(filepath))
        ) {
            fits.addHDU(image);
            fits.write(out);

            return filepath;

        } catch (Exception e) {
            throw new IOException("Error saving image", e);
        }
    }


    //#region header add value helpers
    protected void addValueIfValid(ImageHDU img, String key, String value, String comment) {
        if (value != null) {
            try {
                img.addValue(key, value, comment);
            } catch (HeaderCardException e) {
                // Ignore
            }
        }
    }

    protected void addValueIfValid(ImageHDU img, String key, boolean value, String comment) {
        try {
            img.addValue(key, value, comment);
        } catch (HeaderCardException e) {
            // Ignore
        }
    }

    protected void addValueIfValid(ImageHDU img, String key, int value, String comment) {
        try {
            img.addValue(key, value, comment);
        } catch (HeaderCardException e) {
            // Ignore
        }
    }

    protected void addValueIfValid(ImageHDU img, String key, double value, String comment) {
        if (!Double.isNaN(value)) {
            try {
                img.addValue(key, value, comment);
            } catch (HeaderCardException e) {
                // Ignore
            }
        }
    }
    //#endregion

    //#region header add device values
    protected void addTelescopeValues(ImageHDU img, TelescopeStatus status, boolean addRaDec) {
        if (addRaDec) {
            addValueIfValid(img, "OBJCTRA", status.rightAscension(), "Right Ascension of the object, in decimal hours");
            addValueIfValid(img, "OBJCTDEC", status.declination(), "Declination of the object, in decimal degrees");
        }
        addValueIfValid(img, "OBJCTALT", status.altitude(), "Altitude of the object, in decimal degrees");
        addValueIfValid(img, "OBJCTAZ", status.azimuth(), "Azimuth of the object, in decimal degrees");
        addValueIfValid(img, "TRACKING", status.tracking(), "True if the telescope is tracking, false otherwise");
    }

    protected void addDomeValues(ImageHDU img, DomeStatus status) {
        addValueIfValid(img, "DMAZ", status.azimuth(), "Azimuth of the dome, in decimal degrees");
        addValueIfValid(img, "DMOPEN", status.shutter(), "Percentage of the dome shutter that is open");
        addValueIfValid(img, "DMSLAVED", status.slaved(), "True if the dome is slaved to the telescope, false otherwise");
    }

    protected void addFocuserValues(ImageHDU img, FocuserStatus status) {
        addValueIfValid(img, "FOCUSPOS", status.position(), "Position of the focuser, in steps");
        addValueIfValid(img, "FOCUSTEM", status.temperature(), "Temperature of the focuser, in degrees Celsius");
        addValueIfValid(img, "FOCUSTCP", status.tempComp(), "Temperature compensation of the focuser");
    }

    protected void addFilterWheelValues(ImageHDU img, FilterWheelStatus status) {
        addValueIfValid(img, "FILTER", status.curName(), "Name of selected filter");
        addValueIfValid(img, "FILTEROFF", status.curOffset(), "Focus offset of selected filter");
    }

    @SuppressWarnings("java:S3776")
    protected void addWeatherWatchValues(ImageHDU img, WeatherWatchStatus status, WeatherWatchCapabilities capabilities) {
        final int asString = WeatherWatchService.CAPABILITIES_GENERAL;
        final int asValue = WeatherWatchService.CAPABILITIES_SPECIFIC;

        addValueIfValid(img, "WWISSAFE",status.isSafe(), "True if the weather is safe, false otherwise");
        
        if (capabilities.canCloud() == asString)
            addValueIfValid(img, "WWCLOUDS", status.cloudCover(), "Cloud cover, either \"Clear\", \"Cloudy\" or \"Overcast\"");
        else if (capabilities.canCloud() == asValue)
            addValueIfValid(img, "AOCCLOUD", status.cloudCover(), "ASCOM Observatory Conditions - Cloud coverage in percent");

        if (capabilities.canHumidity() == asString)
            addValueIfValid(img, "WWHUMID", status.humidity(), "Humidity, either \"Dry\", \"Normal\" or \"Humid\"");
        else if (capabilities.canHumidity() == asValue)
            addValueIfValid(img, "AOCHUM", status.humidity(), "ASCOM Observatory Conditions - Humidity in percent");
        
        if (capabilities.canPressure() == asString)
            addValueIfValid(img, "WWBAROM", status.pressure(), "Pressure, either \"Low\", \"Normal\" or \"High\"");
        else if (capabilities.canPressure() == asValue)
            addValueIfValid(img, "AOCBAROM", status.pressure(), "ASCOM Observatory Conditions - Pressure in hPa");

        if (capabilities.canTemperature() == asString) {
            addValueIfValid(img, "WWSKYT", status.temperatureSky(), "Sky temperature (usually mesured using IR), either \"Hot\", \"Cold\" or  \"Normal\"");
            addValueIfValid(img, "WWAMBT", status.temperatureAmbient(), "Ambient temperature, either \"Hot\", \"Cold\" or \"Normal\"");          
        } else if (capabilities.canTemperature() == asValue) {
            addValueIfValid(img, "AOCSKYT", status.temperatureSky(), "ASCOM Observatory Conditions - Sky temperature in degrees C");
            addValueIfValid(img, "AOCAMBT", status.temperatureAmbient(), "ASCOM Observatory Conditions - Ambient temperature in degrees C");
        }

        if (capabilities.canRain() == asString)
            addValueIfValid(img, "WWRAIN", status.rainRate(), "Rain, either \"Dry\", \"Wet\" or \"Rain\"");
        else if (capabilities.canRain() == asValue)
            addValueIfValid(img, "AOCRAIN", status.rainRate(), "ASCOM Observatory Conditions - Rain rate in mm/hour");

        if (capabilities.canWind() == asString) {
            addValueIfValid(img, "WWWIND", status.windSpeed(), "Wind speed, either \"Calm\", \"Windy\" or \"Very windy\"");
            addValueIfValid(img, "WWWINDG", status.windGust(), "Wind gust");
            addValueIfValid(img, "WWWINDD", status.windDirection(), "Wind direction, using cardinal directions, or \"None\"");
        } else if (capabilities.canWind() == asValue) {
            addValueIfValid(img, "AOCWIND", status.windSpeed(), "ASCOM Observatory Conditions - Wind speed in m/s");
            addValueIfValid(img, "AOCWINDG", status.windGust(), "ASCOM Observatory Conditions - Wind gust in m/s");
            addValueIfValid(img, "AOCWINDD", status.windDirection(), "ASCOM Observatory Conditions - Wind direction in degrees");
        }

        if (capabilities.canSkyQuality() == asString)
            addValueIfValid(img, "WWSKYQU", status.skyQuality(), "Sky quality, either \"Good\", \"Normal\" or \"Poor\"");
        else if (capabilities.canSkyQuality() == asValue)
            addValueIfValid(img, "AOCSKYQU", status.skyQuality(), "ASCOM Observatory Conditions - Sky quality in magnitudes per square arcsecond");

        if (capabilities.canSkyBrightness() == asString)
            addValueIfValid(img, "WWSKYBR", status.skyBrightness(), "Sky brightness, either \"Dark\", \"Bright\" or \"Very bright\"");
        else if (capabilities.canSkyBrightness() == asValue)
            addValueIfValid(img, "AOCSKYBR", status.skyBrightness(), "ASCOM Observatory Conditions - Sky brightness in Lux");        
    }
    //#endregion

    //#endregion

    //#endregion
    /////////////////////////////// WORKERS ///////////////////////////////////
    //#region Workers

    /**
     * Altair manual slaving worker, every 30 seconds tries to update the
     * position of the dome to match the telescope's position.
     */
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    @SuppressWarnings("java:S3776")
    public void slaver() {
        // If it's not using Altair slaving, do nothing
        if (!(useAltairSlaving.get() && altairSlaved.get())) {
            logger.debug("Slaver: Not using Altair slaving, skipping update");
            altairSlaved.set(false);
            return;
        }
            
        Mono<TelescopeCapabilities> telescopeCapsMono = telescope.getCapabilities();
        Mono<DomeCapabilities> domeCapsMono = dome.getCapabilities();
        Mono<TelescopeStatus> telescopeStatusMono = telescope.getStatus();
        Mono<DomeStatus> domeStatusMono = dome.getStatus();

        // Get the coordinates to slew to, if any
        Mono<Tuple2<Double, Double>> whereTo = Mono
            .zip(telescopeCapsMono, domeCapsMono, telescopeStatusMono, domeStatusMono)
            .flatMap(tuples -> {
                TelescopeCapabilities tsCaps = tuples.getT1();
                DomeCapabilities dmCaps = tuples.getT2();
                TelescopeStatus tsStatus = tuples.getT3();
                DomeStatus dmStatus = tuples.getT4();

                boolean tsOn = tsStatus.connected();
                boolean dmOn = dmStatus.connected();
                boolean dmIsParked = dmStatus.parked();
                boolean dmIsSlewing = dmStatus.slewing();
                boolean syncAz = tsCaps.canSlew() && dmCaps.canSetAzimuth();

                // If the telescope or the dome are not connected, or the dome is parked or
                // slewing, do nothing
                if (!tsOn || !dmOn || dmIsParked || dmIsSlewing)
                    return Mono.just(Tuples.of(Double.NaN, Double.NaN));

                double alt;
                if (dmCaps.canSetAltitude()) {
                    if (tsCaps.canSlew()) {
                        alt = tsStatus.altitude();
                    } else 
                        alt = 90;
                } else {
                    alt = Double.NaN;
                }
                double az = syncAz ? tsStatus.azimuth() : Double.NaN;
                return Mono.just(Tuples.of(alt, az));
            });
        
        
        // Slew the dome to the coordinates
        whereTo.flatMap(altAz -> {
            Double alt = altAz.getT1();
            Double az = altAz.getT2();

            Mono<Boolean> slewAlt = Mono.just(true);
            Mono<Boolean> slewAz = Mono.just(true);

            if (alt != null && !Double.isNaN(alt)) {
                slewAlt = dome.setAltAwait(alt);
                logger.debug("Slaver: Slewing dome to Altitude: {}°", alt);
            }
            if (az != null && !Double.isNaN(az)) {
                slewAz = dome.slewAwait(az);
                logger.debug("Slaver: Slewing dome to Azimuth: {}°", az);
            }

            return Mono
                .whenDelayError(slewAlt, slewAz).thenReturn(true)
                .doOnSuccess(s -> logger.info("Slaver: Successfully slewed dome to Altitude: {}°, Azimuth: {}°", alt, az))
                .doOnError(e -> logger.error("Slaver: Couldn't slew dome to target", e));
        })
        .block(Duration.ofMinutes(5));
    }

    //#endregion
    //////////////////////////// STATUS REPORTING /////////////////////////////
    //#region Status Reporting

    /**
     * Gets the current status of the observatory.
     * @return The current status of the observatory.
     * @throws DeviceException If there was an error getting the status.
     */
    public Mono<ObservatoryStatus> getStatus() throws DeviceException {
        return Mono.zip(
            telescope.getStatus(),
            dome.getStatus(),
            focuser.getStatus(),
            camera.getStatus(),
            filterWheel.getStatus(),
            weatherWatch.getStatus()
        ).map(tuple -> new ObservatoryStatus(
            useAltairSlaving.get(),
            altairSlaved.get(),
            tuple.getT1(),
            tuple.getT2(),
            tuple.getT3(),
            tuple.getT4(),
            tuple.getT5(),
            tuple.getT6()
        )); 
    }

    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records

    public record ObservatoryStatus(
        boolean useAltairSlaving,
        boolean slaved,
        TelescopeStatus telescope,
        DomeStatus dome,
        FocuserStatus focuser,
        CameraStatus camera,
        FilterWheelStatus filterWheel,
        WeatherWatchStatus weatherWatch
    ) {}

    //#endregion

}