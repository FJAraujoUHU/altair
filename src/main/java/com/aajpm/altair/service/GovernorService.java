package com.aajpm.altair.service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aajpm.altair.entity.*;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.service.observatory.DomeService;
import com.aajpm.altair.utility.Interval;
import com.aajpm.altair.utility.solver.EphemeridesSolver;

import nom.tam.fits.FitsException;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;


@Service
public class GovernorService {

    /////////////////////////////// ATTRIBUTES /////////////////////////////////
    //#region Attributes

    private Order currentOrder = null;

    private Interval currentOrderInterval = null;

    private Interval nextNight = null;


    //#region Flags    

    /** If the governor should run */
    private final AtomicBoolean enabledFlag = new AtomicBoolean(false);

    /** If the admin is in control */
    private final AtomicBoolean adminMode = new AtomicBoolean(false);

    /** If the observatory is performing a task */
    private final AtomicBoolean busyFlag = new AtomicBoolean(false);

    /** isSafe() disable override */
    private final AtomicBoolean safeFlag = new AtomicBoolean(false);

    //#endregion

    //#endregion
    ///////////////////// SUPPORTING SERVICES & COMPONENTS ////////////////////
    //#region Supporting Services & Components

    @Autowired
    private ObservatoryService observatoryService;

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

    private final Logger logger = LoggerFactory.getLogger(GovernorService.class);

    //#endregion
    ///////////////////////////// CONSTRUCTORS ////////////////////////////////
    //#region Constructors

    public GovernorService() {
        super();
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
        if (busyFlag.get())
            return Mono.just(State.BUSY);
        
        return observatoryService.getStatus().map(status -> {
            
            boolean allDisconnected = !status.telescope().connected() &&
                                        !status.dome().connected() &&
                                        !status.camera().connected() &&
                                        !status.focuser().connected() &&
                                        !status.filterWheel().connected() &&
                                        !status.weatherWatch().connected();

            boolean isParked = status.telescope().parked() &&
                                status.dome().parked() &&
                                (status.dome().shutter() == DomeService.SHUTTER_CLOSED);

            if (allDisconnected)
                return State.OFF;
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
     * Get the user whose order is currently being executed.
     * 
     * @return the user whose order is currently being executed,
     *         or null if no order is being executed.
     */
    public AltairUser getCurrentUser() {
        return currentOrder == null ? null : currentOrder.getUser();   
    }

    /**
     * @return the current order being executed, or null if no order is being executed.
     */
    public Order getCurrentOrder() {
        return currentOrder;
    }

    //#endregion
    ///////////////////////////// SETTERS/ACTIONS /////////////////////////////
    //#region Setters/Actions


    //#region Job stuff

    /**
     * Starts the current order.
     * 
     * Note that this method will not check for safety, so it should only be called
     * after making sure the outside conditions are safe.
     * 
     * @param order the order to start
     */
    public void startOrder(Order order) {
        try {
            busyFlag.set(true);

            if (currentOrder != null) {
                abortOrder();
            }

            currentOrder = order;

            if (currentOrder instanceof ControlOrder) {
                startControlOrder((ControlOrder) currentOrder);
            } else if (currentOrder instanceof ProgramOrder) {
                startProgramOrder((ProgramOrder) currentOrder);
            } else {
                throw new UnsupportedOperationException("Order " + order.getId() + " type not supported");
            }

        } catch (Exception e) {
            busyFlag.set(false);
            throw e;
        }
    }

    //#region startOrder() helpers
    private void startControlOrder(ControlOrder order) {
        currentOrder = order;
        currentOrderInterval = order.getRequestedInterval();
        busyFlag.set(false);
    }

    private void startProgramOrder(ProgramOrder order) {
        try {
            currentOrder = order;
            Program program = order.getProgram();
            AstroObject target = program.getTarget();
            ExposureOrder exposureOrder = order
                                        .getExposureOrders()
                                        .stream()
                                        .filter(eo -> !eo.isCompleted())
                                        .findFirst()
                                        .orElse(null);
                
            if (exposureOrder == null) {
                throw new IllegalArgumentException("Program " + program.getId() + " has no pending exposure order");
            }
            
            ExposureParams params = exposureOrder.getExposureParams();
            

            Mono<Void> job = observatoryService.startCamera();
            if (target.shouldHaveRaDec()) {
                job = job.then(observatoryService
                            .slewTogetherRaDecAwait(target.getRa(), target.getDec()));
            } else {
                job = job.then(ephemeridesSolver.getAltAz(target.getName()).flatMap(altAz ->
                                observatoryService.slewTogetherAltAzAwait(altAz[0], altAz[1])));
            }

            job = job
                    .then(observatoryService.setSlavingAwait(true))
                    .then(observatoryService.startExposure(params))
                    .doOnTerminate(() -> {
                        long exposureTime = Math.round(params.getExposureTime());
                        currentOrderInterval = new Interval(Instant.now(), Duration.of(exposureTime, ChronoUnit.SECONDS));
                        busyFlag.set(false);
                    });

            job.subscribe();

        } catch (Exception e) {
            currentOrder = null;
            currentOrderInterval = null;
            busyFlag.set(false);
            throw e;
        }
    }
    //#endregion

    public void abortOrder() {
        try {
            busyFlag.set(true);

            observatoryService.abort()
                .doOnTerminate(() -> {
                    markAsFail();
                    currentOrder = null;
                    currentOrderInterval = null;
                    busyFlag.set(false);
                }).subscribe();

        } catch (Exception e) {
            currentOrder = null;
            currentOrderInterval = null;
            busyFlag.set(false);
            throw e;
        }
    }

    public Mono<Void> abortOrderMono() {
        return observatoryService.abort()
                .doOnSubscribe(s -> busyFlag.set(true))
                .doOnTerminate(() -> {
                    markAsFail();
                    currentOrder = null;
                    currentOrderInterval = null;
                    busyFlag.set(false);
                });
    }

    public void endOrder() {
        try {
            busyFlag.set(true);

            observatoryService.abort()
                .doOnTerminate(() -> {

                    markAsComplete();

                    currentOrder = null;
                    currentOrderInterval = null;
                    busyFlag.set(false);
                }).subscribe();

        } finally {
            busyFlag.set(false);
        }
    }

    private void markAsComplete() {
        double exposureTime = currentOrderInterval.getDuration().getSeconds();
        if (currentOrder instanceof ProgramOrder) {
            ProgramOrder programOrder = (ProgramOrder) currentOrder;
            // Find the exposure order that was just completed
            ExposureOrder exposureOrder = programOrder
                                            .getExposureOrders()
                                            .stream()
                                            .filter(eo -> 
                                                !eo.isCompleted() &&
                                                (Double.compare(eo.getExposureParams().getExposureTime(), exposureTime) == 0))
                                            .findFirst()
                                            .orElse(null);
            // If found, mark it as completed
            if (exposureOrder != null) {
                exposureOrder.setState(ExposureOrder.States.COMPLETED);
                ExposureOrder savedExposureOrder = exposureOrderService.update(exposureOrder);
                
                // Check if all exposure orders are completed
                boolean allCompleted = programOrder
                                        .getExposureOrders()
                                        .stream()
                                        .allMatch(ExposureOrder::isCompleted);

                if (allCompleted) {
                    programOrder.setCompleted(true);
                    programOrder = programOrderService.update(programOrder);
                }

                // Store image
                observatoryService.saveImage(programOrder.getProgram().getTarget(), programOrder.getUser())
                    .subscribe(path -> {
                        try {
                            AstroImage dbImage = astroImageService.create(path);
                            dbImage.setExposureOrder(savedExposureOrder);
                            astroImageService.save(dbImage);
                        } catch (FitsException | IOException e) {
                            logger.error("Error saving image", e);
                        }
                    });
            }
        } else {
            // If it's a control order, just mark it as completed
            currentOrder.setCompleted(true);
            currentOrder = orderService.update(currentOrder);
        }
    }

    private void markAsFail() {
        double orderTime = currentOrderInterval.getDuration().getSeconds();
        if (currentOrder instanceof ProgramOrder) {
            ProgramOrder programOrder = (ProgramOrder) currentOrder;
            ExposureOrder exposureOrder = programOrder
                                            .getExposureOrders()
                                            .stream()
                                            .filter(eo -> 
                                                !eo.isCompleted() &&
                                                (Double.compare(eo.getExposureParams().getExposureTime(), orderTime) == 0))
                                            .findFirst()
                                            .orElse(null);
            if (exposureOrder != null) {
                exposureOrder.setState(ExposureOrder.States.FAILED);
                exposureOrderService.update(exposureOrder);
            }
        }
    }


    //#endregion

    //#endregion
    /////////////////////////////// WORKERS ///////////////////////////////////
    //#region Workers

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void governor() {

        if (!enabledFlag.get()) { // If the governor is disabled, do nothing
            return;
        }

        long timeout = 10000;

        // Check if next night is cached. If not, fetch it.
        // * Note: this is done because the night time is not expected to change
        // and fetching might be a costly operation.
        if (this.nextNight == null || this.nextNight.isBefore(Instant.now())) {
            this.nextNight = ephemeridesSolver
                                .getNightTime()
                                .blockOptional(Duration.ofMillis(timeout))
                                .orElse(null);
        }

        Mono<Boolean> isSafeMono = observatoryService
                                    .isSafe()
                                    .onErrorReturn(false);
        Mono<State> stateMono = this
                                    .getState()
                                    .onErrorReturn(State.ERROR);
        Tuple2<Boolean, State> t = Mono
                                    .zip(isSafeMono, stateMono)
                                    .blockOptional(Duration.ofMillis(timeout))
                                    .orElse(Tuples.of(false, State.ERROR));

        boolean isSafe = Boolean.TRUE.equals(t.getT1()) || safeFlag.get();
        boolean isDone = currentOrderInterval == null || currentOrderInterval.isBefore(Instant.now());
        State state = t.getT2();

        logger.info("Governor executing: isSafe = {}, state = {}", isSafe, state);

        switch (state) {
            case DISABLED:
            case BUSY:
            case ADMIN:
                // If on one of these states, just exit
                return;
            case OFF:
                /**
                 * Check if it's time to start the scheduler,
                 * and if so, connect and start running programs.
                 */
                offStateAction(isSafe);
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
                logger.warn("Governor is in an error state: {}", state);
                return;
        }
    }

    /**
     * Check if it's time to start the scheduler,
     * and if so, connect and start running programs.
     */
    private void offStateAction(boolean isSafe) {
        try {
            if (!isSafe || nextNight == null || !nextNight.contains(Instant.now())) {
                // It's not time yet, exit
                return;
            }

            Interval searchInterval = new Interval(Instant.now(), nextNight.getEnd());
            List<Order> orders = orderService.buildSchedule(searchInterval).block();
            if (orders == null || orders.isEmpty()) {
                // No orders to run, exit
                return;
            }

            // Start the observatory and start the first order
            observatoryService.startAwait().block(Duration.ofMinutes(5));
            this.startOrder(orders.get(0));

        } catch (Exception e) {
            logger.error("Error starting the observatory", e);
        }
    }

    /**
     * Check if it's time to start the scheduler,
     * and if so, connect and start running programs.
     */
    private void parkedStateAction(boolean isSafe) {
        try { 
            if (!isSafe || nextNight == null || !nextNight.contains(Instant.now())) {
                observatoryService.stopAwait()
                        .then(observatoryService.disconnectAll())
                        .block(Duration.ofMinutes(5));
                return;
            }

            Interval searchInterval = new Interval(Instant.now(), nextNight.getEnd());
            List<Order> orders = orderService.buildSchedule(searchInterval).block();
            if (orders == null || orders.isEmpty()) {
                // No orders to run, turn off
                observatoryService.stopAwait()
                        .then(observatoryService.disconnectAll())
                        .block(Duration.ofMinutes(5));
                return;
            }

            // Start the observatory and start the first order
            observatoryService.startAwait().block(Duration.ofMinutes(5));
            this.startOrder(orders.get(0));

        } catch (Exception e) {
            logger.error("Error starting the observatory", e);
        }
    }

    /**
     * Check if isSafe, if not, go into PARKED.
     * Check if there are programs to run.
     */
    private void idleStateAction(boolean isSafe) {
        // If unsafe, go to PARKED
        if (!isSafe || nextNight == null || !nextNight.contains(Instant.now())) {
            observatoryService.stopAwait()
                .block(Duration.ofMinutes(5));
            return;
        }
        
        // If there are no orders, park and exit
        Interval searchInterval = new Interval(Instant.now(), nextNight.getEnd());
        List<Order> orders = orderService.buildSchedule(searchInterval).block();
        if (orders == null || orders.isEmpty()) {
            observatoryService.stopAwait()
                .block(Duration.ofMinutes(5));
            return;
        }

        // Start the observatory and start the first order
        observatoryService.startCamera().block(Duration.ofMinutes(3));
        this.startOrder(orders.get(0));
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

            } else {
                // Go to PARKED/Safe position
                observatoryService.stopAwait()
                    .block(Duration.ofMinutes(5));
            }

            // Download and save the image, mark the exposure/program as complete
            this.endOrder();

        } else {
            if (isSafe) {
                // It's safe and running, so do nothing

            } else {
                // Abort and go to PARKED/Safe position
                this.abortOrderMono()
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

            } else {
                // Go to PARKED/Safe position
                observatoryService.stopAwait()
                    .block(Duration.ofMinutes(5));
            }

            this.endOrder();
        } else {
            if (isSafe) {
                // It's safe and running, so do nothing
            } else {
                // Abort and go to PARKED/Safe position
                this.abortOrderMono()
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
        /** An admin has disabled Altair. Same as OFF, but the scheduler should not start. */
        DISABLED,
        /** Observatory is off and device services are disconnected (should allow other apps to connect). */
        OFF,
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

    //#endregion

    
}
