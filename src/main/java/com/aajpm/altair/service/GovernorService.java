package com.aajpm.altair.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.aajpm.altair.config.ObservatoryConfig;
import com.aajpm.altair.entity.*;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;
import com.aajpm.altair.service.observatory.DomeService;
import com.aajpm.altair.service.observatory.WeatherWatchService;
import com.aajpm.altair.utility.Interval;
import com.aajpm.altair.utility.exception.DeviceUnavailableException;
import com.aajpm.altair.utility.exception.UnauthorisedException;
import com.aajpm.altair.utility.solver.EphemeridesSolver;

import nom.tam.fits.FitsException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuple5;
import reactor.util.function.Tuples;

@Service
public class GovernorService {

    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    private Order currentOrder = null;

    private Order nextOrder = null;

    private Interval currentOrderInterval = null;

    private Interval nextNight = null;

    private AltairUser currentUser = null;

    //#region Flags    

    /** If the governor should run */
    private final AtomicBoolean enabledFlag = new AtomicBoolean(false);

    /** If the admin is in control */
    private final AtomicBoolean adminMode = new AtomicBoolean(false);

    /** isSafe() disable override */
    private final AtomicBoolean safeFlag = new AtomicBoolean(false);

    //#endregion

    //#endregion
    ///////////////////// SUPPORTING SERVICES & COMPONENTS ////////////////////
    //#region Supporting Services & Components

    @Autowired
    private ObservatoryService observatoryService;

    @Autowired
    private WeatherWatchService weatherWatch;

    @Autowired
    private EphemeridesSolver ephemeridesSolver;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProgramOrderService programOrderService;

    @Autowired
    private ExposureOrderService exposureOrderService;

    @Autowired
    private AstroImageService astroImageService;

    @Autowired
    private AltairUserService userService;

    private final Logger logger = LoggerFactory.getLogger(GovernorService.class);

    //#endregion
    ///////////////////////////// CONSTRUCTORS ////////////////////////////////
    //#region Constructors

    public GovernorService(ObservatoryConfig config) {
        super();
        safeFlag.set(config.getDisableSafetyChecks());
    }

    //#endregion
    //////////////////////////////// GETTERS //////////////////////////////////
    //#region Getters

    /**
     * @return the current state of the governor.
     */
    @SuppressWarnings("java:S3776")
    public Mono<State> getState() {
        if (adminMode.get())
            return Mono.just(State.ADMIN);
        if (!enabledFlag.get())
            return Mono.just(State.DISABLED);
        
        return observatoryService.getStatus().map(status -> {

            boolean isParked = status.telescope().parked() &&
                                status.dome().parked() &&
                                DomeService.SHUTTER_CLOSED_STATUS.equalsIgnoreCase(status.dome().shutterStatus());
            
            boolean isOff = !status.telescope().connected() &&
                            !status.dome().connected() &&
                            !status.camera().connected() &&
                            !status.focuser().connected() &&
                            !status.filterWheel().connected();

            if (isOff)
                return State.PARKED;
            if (currentOrder == null)
                return isParked ? State.PARKED : State.IDLE;
            if (currentOrder instanceof ControlOrder)
                return State.MANUAL;
            if (currentOrder instanceof ProgramOrder)
                return State.RUNNING_PROGRAM;

            return State.ERROR;
        }); 
    }

    /**
     * Gets the current status of the governor.
     * @return the current status of the governor.
     */
    public Mono<GovernorStatus> getStatus() {
        Mono<String> stateMono = getState().map(state -> state.toString().toLowerCase()).onErrorReturn("ERROR");
        Mono<Boolean> isSafeMono = observatoryService.isSafe(false).onErrorReturn(false);
        Mono<Boolean> isSafeOverrideMono = Mono.just(safeFlag.get());

         Mono<String> currentOrderMono = Mono.fromCallable(() -> {
            if (currentOrder instanceof ProgramOrder) {
                ProgramOrder programOrder = (ProgramOrder) currentOrder;
                return String.format(Locale.US,"%s [ID: %d]", programOrder.getProgram().getName(), programOrder.getId());
            } else if (currentOrder != null) {
                return String.format(Locale.US,"[ID: %d]", currentOrder.getId());
            }
            return "None";
        });

        Mono<String> currentOrderRemainingTimeMono = Mono.just((currentOrderInterval == null || currentOrderInterval.hasElapsed())
                                                        ? "N/A"
                                                        : (new Interval(Instant.now(), currentOrderInterval.getEnd()).toDurationString()));

        Mono<String> nextOrderMono = Mono.fromCallable(() -> {
            if (nextOrder instanceof ProgramOrder) {
                ProgramOrder programOrder = (ProgramOrder) nextOrder;
                return String.format(Locale.US,"%s [ID: %d]", programOrder.getProgram().getName(), programOrder.getId());
            } else if (nextOrder != null) {
                return String.format(Locale.US,"[ID: %d]", nextOrder.getId());
            }
            return "None";
        });

        Mono<String> currentUserMono = Mono.just(getCurrentUser() == null ? "None" : getCurrentUser().getUsername());
       
        Mono<String> nextNightMono = Mono.just(nextNight == null ? "None" : nextNight.toString());

        Mono<String> useAltairSlaving = observatoryService.isAltairSlaving()
                                        .map(use -> Boolean.TRUE.equals(use) ? "Altair":"Native").onErrorReturn("Unknown");
        Mono<String> isSlaved = observatoryService.isSlaved().onErrorReturn(false)
                                        .map(slaved -> Boolean.TRUE.equals(slaved) ? "Slaved":"Not slaved").onErrorReturn("Unknown");
        Mono<String> slavingMono = useAltairSlaving.zipWith(isSlaved, (use, slaved) -> String.format(Locale.US,"%s (%s)", slaved, use));

        
        Mono<Tuple4<String, Boolean, Boolean, String>> stateMono1 = Mono.zip(stateMono, isSafeMono, isSafeOverrideMono, currentOrderMono);
        Mono<Tuple5<String, String, String, String, String>> stateMono2 = Mono.zip(currentOrderRemainingTimeMono, nextOrderMono, currentUserMono, nextNightMono, slavingMono);
        return stateMono1.zipWith(stateMono2).map(tuple -> new GovernorStatus(
            tuple.getT1().getT1(),
            tuple.getT1().getT2(),
            tuple.getT1().getT3(),
            tuple.getT1().getT4(),
            tuple.getT2().getT1(),
            tuple.getT2().getT2(),
            tuple.getT2().getT3(),
            tuple.getT2().getT4(),
            tuple.getT2().getT5()
        ));
    }

    /**
     * Get the user whose order is currently being executed.
     * 
     * @return the user whose order is currently being executed,
     *         or null if no order is being executed.
     */
    public AltairUser getCurrentUser() {
        if (currentOrder == null) {
            return currentUser;
        } else {
            return currentOrder.getUser();
        }
    }

    /**
     * @return the current order being executed, or null if no order is being executed.
     */
    public Order getCurrentOrder() {
        return currentOrder;
    }

    /**
     * @return If the governor is enabled
     */
    public boolean isEnabled() {
        return enabledFlag.get();
    }

    /**
     * Checks if the given user is allowed to operate the observatory right now.
     * <p>
     * To be able to operate the observatory, the user must be registered in the
     * database and satisfy at least one of the following conditions:
     * <p><ul>
     * <li>Be an admin user</li>
     * <li>Be the current user and the current order is a control order that
     * hasn't expired</li>
     * </ul><p>
     * 
     * @param user the user to check
     * @return {@code true} if the user is allowed to operate the observatory right now,
     *         and {@code false} otherwise.
     */
    public boolean userCanOperate(AltairUser user) {
        if (user == null)
            return false;
        try {
            AltairUser dbUser = (AltairUser) userService.loadUserByUsername(user.getUsername());
            boolean isUserReal = dbUser != null && dbUser.equals(user);
            boolean isAdmin = user.isAdmin();
            boolean isCurrentUser = currentUser != null && currentUser.equals(user);
            boolean isCurrentOrderAControlOrder = currentOrder instanceof ControlOrder;
            boolean hasCurrentOrderExpired = currentOrderInterval != null && currentOrderInterval.hasElapsed();

            return isUserReal && (isAdmin || (isCurrentUser && isCurrentOrderAControlOrder && !hasCurrentOrderExpired));
        } catch (UsernameNotFoundException e) {
            return false;
        }     
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions

    /**
     * Enables the governor.
     */
    public Mono<Boolean> enable() {
        return weatherWatch.connect().doOnSuccess(v -> enabledFlag.set(true));
    }

    /**
     * Disables the governor.
     */
    public Mono<Boolean> disable() {
        return disconnectAll().doOnSuccess(v -> enabledFlag.set(false));
    }

    /**
     * Connects all the devices of the observatory.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Boolean> connectAll() {
        return observatoryService.connectAll();
    }

    /**
     * Disconnects all the devices of the observatory.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Boolean> disconnectAll() {
        return observatoryService.disconnectAll();
    }

    /**
     * Disconnects all the devices of the observatory except the weather station.
     * 
     * @return A {@link Mono} that will complete inmidiately.
     *         If an error occurs, it will complete with an error.
     */
    public Mono<Boolean> disconnectAllExceptWeather() {
        return observatoryService.disconnectAllExceptWeather();
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
    public Mono<Boolean> startObservatory() {
        return observatoryService.start();
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
    public Mono<Boolean> stopObservatory() {
        return observatoryService.stop();
    }


    /**
     * Sets the governor to admin mode.
     * 
     * @param admin the admin user.
     * @throws UnauthorisedException if the user is not an admin.
     */
    public Mono<Boolean> enterAdminMode(AltairUser admin) throws UnauthorisedException {
        if (admin == null)
            return Mono.error(new UnauthorisedException("Must provide an admin user"));
        if (!admin.isAdmin())
            return Mono.error(new UnauthorisedException(admin));

        return this.abortOrder()
            .onErrorResume(e -> {
                logger.error("Governor::enterAdminMode(): Error while aborting order", e);
                return Mono.just(true);
            })
            .then(Mono.fromCallable(() -> {
                currentUser = admin;
                adminMode.set(true);
                return true;
            }));
    }

    /**
     * Exits admin mode.
     * 
     * @param admin the admin user.
     * @throws IllegalStateException if not already in admin mode.
     * @throws UnauthorisedException if the user is not an admin.
     */
    public Mono<Boolean> exitAdminMode(AltairUser admin) throws IllegalStateException, UnauthorisedException {
        if (!adminMode.get())
            return Mono.error(new IllegalStateException("Not in admin mode"));
        if (admin == null)
            return Mono.error(new UnauthorisedException("Must provide an admin user"));
        if (!admin.isAdmin())
            return Mono.error(new UnauthorisedException(admin));
        
        return Mono.fromCallable(() -> {
            currentUser = null;
            adminMode.set(false);
            return true;
        });
    }

    /**
     * Sets whether the governor should ignore the safety checks.
     * 
     * NOTE: This might be dangerous, so use with caution.
     * @param ignoreIsSafe whether the governor should ignore the safety checks.
     */
    public void setSafeOverride(boolean ignoreIsSafe) {
        safeFlag.set(ignoreIsSafe);
    }

    /**
     * Enables/disables dome slaving.
     * 
     * @param slaving If true, the dome will be slaved to the telescope.
     *                If false, the dome will be free to move independently.
     * 
     * @return A {@link Mono} that will complete as soon as the changes apply.
     */
    public Mono<Boolean> setSlaving(boolean slaved) {
        return observatoryService.setSlaved(slaved);
    }

    /**
     * Enables/disables dome slaving.
     * 
     * @param slaving If true, the dome will be slaved to the telescope.
     *                If false, the dome will be free to move independently.
     * 
     * @return A {@link Mono} that will complete as soon as the dome syncs and the changes apply.
     */
    public Mono<Boolean> setSlavingAwait(boolean slaved) {
        return observatoryService.setSlavedAwait(slaved);
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
        return observatoryService.useAltairSlaving(useAltairSlaving);
    }

    /**
     * Starts a new program, by placing a new order for it and starting it right away.
     * 
     * @param program the program to start
     * @param user the user that is starting the program
     * @throws UnauthorisedException if the user does not have permission to start the program
     * @throws IllegalArgumentException if the program or user is null
     * @throws IllegalStateException if there is already an order in progress
     */
    public Mono<Boolean> startProgram(Program program, AltairUser user) {
        if (program == null)
            return Mono.error(new IllegalArgumentException("Must provide a program"));
        if (user == null)
            return Mono.error(new IllegalArgumentException("Must provide a user"));
        if (!userCanOperate(user))
            return Mono.error(new UnauthorisedException(user, "Cannot start a program while not operating the observatory"));
        if (currentOrder != null)
            return Mono.error(new IllegalStateException("Cannot start a new order while another one is in progress"));

        return Mono.fromCallable(() -> {
            ProgramOrder order = programOrderService.create();
            order.setProgram(program);
            order.setUser(user);
            order.setCompleted(false);
            order.setCreationTime(Instant.now());
            ProgramOrder setupOrder = order;
            program.getExposures().forEach(e -> {
                ExposureOrder exposureOrder = exposureOrderService.create();
                exposureOrder.setExposureParams(e);
                exposureOrder.setProgram(setupOrder);
                exposureOrder.setState(ExposureOrder.States.PENDING);
                setupOrder.getExposureOrders().add(exposureOrder);
            });

            return programOrderService.save(setupOrder);
        })
        .flatMap(this::queueOrder);
    }

    public Mono<Boolean> queueOrder(Order order) {
        if (order == null)
            return Mono.error(new IllegalArgumentException("Must provide an order"));

        return Mono.fromCallable(() -> {
            nextOrder = order;
            return true;
        });
    }


    /**
     * Starts the order.
     * 
     * Note that this method will not check for safety, so it should only be
     * called after making sure the outside conditions are safe and starting
     * the observatory.
     * 
     * @param order the order to start
     */
    private Mono<Boolean> startOrder(Order order) {
        if (order == null)
            return Mono.error(new IllegalArgumentException("Must provide an order"));

        Mono<Boolean> ret = Mono.empty().flatMap(b -> currentOrder != null 
                                                    ? abortOrder()
                                                    : Mono.just(true));

        if (order instanceof ControlOrder) {
            ret = ret.then(startControlOrder((ControlOrder) order));
        } else if (order instanceof ProgramOrder) {
            ret = ret.then(startProgramOrder((ProgramOrder) order));
        } else {
            return Mono.error(new UnsupportedOperationException("Order " + order.getId() + " type not supported"));
        }

        return ret;
    }

    //#region startOrder helpers
    private Mono<Boolean> startControlOrder(ControlOrder order) {
        return Mono.fromCallable(() -> {
            currentOrder = order;
            currentOrderInterval = order.getRequestedInterval();
            return true;
        });
    }

    private Mono<Boolean> startProgramOrder(ProgramOrder order) {
        // Get the exposure order to run
        Mono<ExposureOrder> exposureOrder = Mono.fromCallable(() -> {
            ExposureOrder eo = order.getExposureOrders()
                                    .stream()
                                    .filter(exp -> !exp.isCompleted())
                                    .sorted((e1, e2) ->
                                        e1.getExposureParams()
                                            .getExposureTime()
                                            .compareTo(
                                                e2.getExposureParams().getExposureTime()
                                            )
                                    )
                                    .findFirst()
                                    .orElse(null);
            if (eo == null)
                throw new IllegalStateException("Program " + order.getProgram().getId() + " has no pending exposure orders");
            return eo;
        });

        // Start the exposure order
        return exposureOrder.flatMap(eo -> {
            AstroObject target = eo.getExposureParams().getProgram().getTarget();
            ExposureParams params = eo.getExposureParams();

            Mono<Boolean> job = observatoryService.startCamera();
                // Slew to the target, whether it's an object or coordinates
                if (target.shouldHaveRaDec()) {
                    job = job.then(observatoryService
                                .slewTogetherRaDecAwait(target.getRa(), target.getDec()));
                } else {
                    job = job.then(ephemeridesSolver.getAltAz(target.getName()).flatMap(altAz ->
                                    observatoryService.slewTogetherAltAzAwait(altAz[0], altAz[1])));
                }
            
             job = job
                    .then(observatoryService.setSlavedAwait(true))
                    .then(observatoryService.startExposure(params))
                    .then(Mono.fromCallable(() -> {
                        eo.setState(ExposureOrder.States.IN_PROGRESS);
                        ExposureOrder savedExposureOrder = exposureOrderService.update(eo);
                        long exposureTime = Math.round(savedExposureOrder.getExposureParams().getExposureTime());
                        currentOrder = savedExposureOrder.getProgram();
                        currentOrderInterval = new Interval(Instant.now(), Duration.of(exposureTime, ChronoUnit.SECONDS));
                        return true;
                    }));
            return job;
        })
        .onErrorResume(e -> Mono.fromCallable(() -> {
            // If there was an error, mark the exposure order as failed
            // and go to IDLE
            if (currentOrder != null) {
                markAsFail();
            }
            currentOrder = null;
            currentOrderInterval = null;
            return true;
        }));
    }
    //#endregion

    /**
     * Aborts the current order, manages the order's status and goes to IDLE.
     * @return a Mono that executes the abort order and completes when the 
     *          order is aborted.
     */
    public Mono<Boolean> abortOrder() {
        return observatoryService.abort()
                .then(Mono.fromCallable(() -> {
                    markAsFail();
                    currentOrder = null;
                    currentOrderInterval = null;
                    return true;
                }));
    }

    /**
     * Ends the current order, manages the order's completion status and goes to IDLE.
     */
    private Mono<Boolean> endOrder() {
        return observatoryService.abort()
                .then(Mono.fromCallable(() -> {
                    markAsComplete();
                    currentOrder = null;
                    currentOrderInterval = null;
                    return true;
                }));
    }

    private void markAsComplete() {
        if (currentOrder instanceof ProgramOrder) {
            ProgramOrder programOrder = (ProgramOrder) currentOrder;
            // Find the exposure order that was running
            ExposureOrder exposureOrder = programOrder
                                            .getExposureOrders()
                                            .stream()
                                            .filter(eo -> eo.getState() == ExposureOrder.States.IN_PROGRESS)
                                            .findFirst()
                                            .orElse(null);
            // If found, mark it as completed and save the image
            if (exposureOrder != null) {
                // Store image
                observatoryService.saveImage(programOrder.getProgram().getTarget(), programOrder.getUser())
                    .subscribe(path -> {
                        try {
                            logger.debug("Governor: Marking exposure {} of order {} as completed", exposureOrder.getId(), programOrder.getId());
                            exposureOrder.setState(ExposureOrder.States.COMPLETED);
                            
                            logger.debug("Governor: Saving image {} for exposure order {}", path, exposureOrder.getId());
                            AstroImage dbImage = astroImageService.create(path);
                            dbImage.setExposureOrder(exposureOrder);
                            exposureOrder.setImage(dbImage);
                            exposureOrder.setState(ExposureOrder.States.COMPLETED);


                            // Check if all exposure orders are completed
                            boolean allCompleted = programOrder
                                                    .getExposureOrders()
                                                    .stream()
                                                    .allMatch(ExposureOrder::isCompleted);

                        if (allCompleted) {
                                logger.debug("Governor: All exposures completed, marking order {} as completed", programOrder.getId());
                                
                                programOrder.setCompleted(true);
                                programOrderService.update(programOrder);
                            } else {
                                exposureOrderService.update(exposureOrder);
                            }
                        } catch (FitsException | IOException e) {
                            logger.error("Error saving image", e);
                        }
                    });
            }
        } else {
            // If it's a control order, just mark it as completed
            logger.debug("Governor: Marking order {} as completed", currentOrder.getId());
            currentOrder.setCompleted(true);
            currentOrder = orderService.update(currentOrder);
        }
    }

    private void markAsFail() {
        if (currentOrder instanceof ProgramOrder) {
            ProgramOrder programOrder = (ProgramOrder) currentOrder;
            // Find the current exposure order and mark it as failed
            ExposureOrder exposureOrder = programOrder
                                            .getExposureOrders()
                                            .stream()
                                            .filter(eo -> eo.getState() == ExposureOrder.States.IN_PROGRESS)
                                            .findFirst()
                                            .orElse(null);
            if (exposureOrder != null) {
                logger.debug("Governor: Marking exposure {} of order {} as failed", exposureOrder, currentOrder.getId());
                exposureOrder.setState(ExposureOrder.States.FAILED);
                exposureOrderService.update(exposureOrder);
            } else {
                logger.warn("Governor: Couldn't find exposure order for order {}", currentOrder.getId());
            }
        }
    }

    //#endregion
    /////////////////////////////// WORKERS ///////////////////////////////////
    //#region Workers

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void governor() {

        if (!enabledFlag.get()) { // If the governor is disabled, do nothing
            return;
        }

        long timeout = 15000;

        // Check if next night is cached. If not, fetch it.
        // * Note: this is done because the night time is not expected to change
        // and fetching might be a costly operation.
        if (this.nextNight == null || this.nextNight.isBefore(Instant.now())) {
            this.nextNight = ephemeridesSolver
                                .getNightTime()
                                .blockOptional(Duration.ofMillis(30000))
                                .orElse(null);
        }
        if (this.nextNight == null) {
            logger.warn("Governor: couldn't find next night, assuming it's not safe to observe.");
        }

        Mono<Boolean> isSafeMono = weatherWatch.connect().then(observatoryService
                                    .isSafe(false)  // will use nextNight if available
                                    .onErrorReturn(false));

        Mono<State> stateMono = this.getState()
                                    .onErrorReturn(State.ERROR);

        Tuple2<Boolean, State> t = Mono
                                    .zip(isSafeMono, stateMono)
                                    .blockOptional(Duration.ofMillis(timeout))
                                    .orElse(Tuples.of(false, State.ERROR));

        boolean isSafe;
        if (safeFlag.get()) {
            logger.info("Governor: Safe override is on, ignoring isSafe() and executing.");
            isSafe = true;
        } else {
            logger.trace("Governor: [isSafe = {}, nextNight = {}, isNight = {}]", t.getT1(), nextNight, nextNight == null ? "null" : nextNight.contains(Instant.now()));
            isSafe = Boolean.TRUE.equals(t.getT1()) && nextNight != null && nextNight.contains(Instant.now());
        }
        
        boolean isDone = currentOrderInterval == null || currentOrderInterval.isBefore(Instant.now());
        State state = t.getT2();

        logger.debug("Governor executing: isSafe = {}, state = {}", isSafe, state);

        switch (state) {
            case DISABLED:
                logger.error("If you're seeing this, something is wrong: governor is disabled but somehow still running.");
                return;
            case ADMIN:
                logger.debug("Governor: Admin mode, skipping execution");
                return;
            case PARKED:
                /* Same as OFF, but if there are no orders/it's not time yet, make sure to
                 * park everything and turn off the observatory.
                 */
                parkedStateAction(isSafe);
                return;
            case IDLE:
                /**
                 * Check if isSafe, if not, go into PARKED.
                 * Check if there are programs to run.
                 */
                idleStateAction(isSafe);
                return;
            case RUNNING_PROGRAM:
                /**
                 * Check if isSafe, if not, abort the program
                 * and go to a safe position.
                 * Check if the program is still running.
                 * If not, mark it as complete, save the image
                 * and start the next one.
                 */
                runningProgramStateAction(isSafe, isDone);
                return;
            case MANUAL:
                /**
                 * Check if the user's session is over.
                 * If so, kick out and park the telescope.
                 */
                manualStateAction(isSafe, isDone);
                return;
            case ERROR:
            default:
                logger.error("Governor is in an error/unknown state: {}", state);
                return;
        }
    }

    /**
     * Check if it's time to start the scheduler,
     * and if so, connect and start running programs.
     */
    private void parkedStateAction(boolean isSafe) {
        try {
            // If it's not safe, make sure it's parked and exit
            if (!isSafe) {
                logger.info("Governor: it's not safe to observe, parking the telescope. [isSafe = {}, nextNight = {}, isNight = {}]", isSafe, nextNight, nextNight == null ? "null" : nextNight.contains(Instant.now()));
                observatoryService.stopAwait()
                        .then(observatoryService.disconnectAllExceptWeather())
                        .block(Duration.ofMinutes(5));
                return;
            }


            Order queuedOrder = null;
            if (nextOrder != null) {
                queuedOrder = nextOrder;
                nextOrder = null;
            } else {
                // It's time to start the scheduler
                Interval searchInterval = new Interval(Instant.now(), nextNight.getEnd());
                List<Order> orders = orderService.buildSchedule(searchInterval).block();
                if (orders != null && !orders.isEmpty()) {
                    queuedOrder = orders.get(0);
                }
            }

            if (queuedOrder == null) {
                // If there are no orders to run, park the telescope and exit
                logger.info("Governor: no orders to run, parking the telescope.");
                    observatoryService.stopAwait()
                            .then(disconnectAllExceptWeather())
                            .block(Duration.ofMinutes(5));
                    return;
            }
            
            // If there are orders to run, start the observatory and run the queued order
            logger.debug("Governor: Starting the observatory and running the first order.");
            connectAll().block(Duration.ofMinutes(5));
            observatoryService.startAwait().block(Duration.ofMinutes(5));
            logger.info("Governor: Observatory started, running the order [ID={}]", queuedOrder);
            this.startOrder(queuedOrder).block(Duration.ofMinutes(5));
        } catch (Exception e) {
            logger.error("Error starting the observatory", e);
        }
    }

    /**
     * Check if isSafe, if not, go into PARKED.
     * Check if there are programs to run.
     */
    private void idleStateAction(boolean isSafe) {
        try {
            if (!isSafe) {
                logger.info("Governor: it's not safe to observe, parking the telescope. [isSafe = {}, nextNight = {}, isNight = {}]", isSafe, nextNight, nextNight == null ? "null" : nextNight.contains(Instant.now()));
                observatoryService.stopAwait()
                    .block(Duration.ofMinutes(5));
                return;
            }
            

            Order queuedOrder = null;
            if (nextOrder != null) {
                queuedOrder = nextOrder;
                nextOrder = null;
            } else {
                // It's time to start the scheduler
                Interval searchInterval = new Interval(Instant.now(), nextNight.getEnd());
                List<Order> orders = orderService.buildSchedule(searchInterval).block();
                if (orders != null && !orders.isEmpty()) {
                    queuedOrder = orders.get(0);
                }
            }

            if (queuedOrder == null) {
                // If there are no orders to run, park the telescope and exit
                logger.info("Governor: no orders to run, parking the telescope.");
                    observatoryService.stopAwait()
                            .then(disconnectAllExceptWeather())
                            .block(Duration.ofMinutes(5));
                    return;
            }
            
            // If there are orders to run, start the observatory and run the queued order
            logger.debug("Governor: Starting the observatory and running the first order.");
            connectAll().block(Duration.ofMinutes(5));
            observatoryService.startAwait().block(Duration.ofMinutes(5));
            logger.info("Governor: Observatory started, running the order [ID={}]", queuedOrder);
            this.startOrder(queuedOrder).block(Duration.ofMinutes(5));
        } catch (Exception e) {
            logger.error("Error starting the observatory", e);
        }
        
    }

    /**
     * Check if the program is done, and download if it is.
     * Check if isSafe, if not, abort the program
     * and go to a safe position.
     * Check if the program is still running.
     * If not, mark it as complete, save the image
     * and start the next one.
     */
    private void runningProgramStateAction(boolean isSafe, boolean isDone) {
        if (isDone) {

            if (isSafe) {
                // Go to IDLE (Do nothing, save the image will clear currentProgram)
                logger.info("Governor: Program is done, saving the image.");

            } else {
                logger.info("Governor: Program is done, but it's not safe to observe, parking the telescope first.");
                observatoryService.stopAwait()
                    .block(Duration.ofMinutes(5));
            }

            // Download and save the image, mark the exposure/program as complete
            this.endOrder()
                .block(Duration.ofMinutes(5));

        } else {
            if (isSafe) {
                // It's safe and running, so do nothing
                logger.debug("Governor: Program is running, waiting for it to finish.");

            } else {
                // Abort and go to PARKED/Safe position
                logger.info("Governor: It's not safe to observe, aborting the program and parking the telescope.");
                this.abortOrder()
                    .then(observatoryService.stopAwait())
                    .block(Duration.ofMinutes(5));
            }
        }
    }

    /**
     * Check if the user's session is over.
     * If so, kick out and park the telescope.
     */
    private void manualStateAction(boolean isSafe, boolean isDone) {
        if (isDone) {
            if (isSafe) {
                // Go to IDLE (Do nothing, endOrder() will clear currentProgram)
                if (currentOrder != null && currentOrder.getUser() != null) {
                    AltairUser user = currentOrder.getUser();
                    logger.info("Governor: {}'s session is over, parking the telescope. [User ID = {}]", user.getUsername(), user.getId());
                } else {
                    logger.info("Governor: User session is over, parking the telescope.");
                }
            } else {
                logger.info("Governor: Session is over, but it's not safe to keep observing, parking the telescope first.");
                observatoryService.stopAwait()
                    .block(Duration.ofMinutes(5));
            }

            this.endOrder()
                .block(Duration.ofMinutes(5));

        } else {
            if (isSafe) {
                // It's safe and running, so do nothing
                 logger.debug("Governor: Session is running, waiting for it to finish.");
            } else {
                // Abort and go to PARKED/Safe position
                logger.info("Governor: It's not safe to observe, aborting the session and parking the telescope.");
                this.abortOrder()
                    .then(observatoryService.stopAwait())
                    .block(Duration.ofMinutes(5));
            }
        }
    }


    //#endregion
    //////////////////////////////// RECORDS //////////////////////////////////
    //#region Records
    /**
     * Represents the status of the observatory.
     */
    public enum State {
        /** An admin has disabled Altair. Devices should be disconnected and free and the scheduler won't run */
        DISABLED,
        /** Telescope and dome parked and ready to be used. */
        PARKED,
        /** Observatory is managed by Altair, but not in use. */
        IDLE,
        /** Observatory is performing a {@link ProgramOrder}. */
        RUNNING_PROGRAM,
        /** Observatory is being used manually by an advanced user. */
        MANUAL,
        /** Observatory is being used manually by an admin. */
        ADMIN,
        /** Observatory is performing a task and should not be interrupted until it finishes */
        BUSY,
        /** Telescope is in an error state and must be reset by an admin or physically fixed. */
        ERROR
    }

    public record GovernorStatus(
        /** Current state of the governor, as a String */
        String state,
        /** ObservatoryService.isSafe() */
        boolean isSafe,
        /** If isSafe should be ignored */
        boolean isSafeOverride,
        /** Current order, as "Name (ID: XX)" */
        String currentOrder,
        /** Remaining time in current order, as {@code Xd Yh Zm As} */
        String currentOrderRemainingTime,
        /** Next order, as "Name (ID: XX)" */
        String nextOrder,
        String currentUser,
        /** Value of nextNight, as DateStart -> DateEnd */
        String nextNight,
        /** Slaving status, as "useAltairSlaving (slaving)" */
        String slaving
    ) {}

    //#endregion

    
}
