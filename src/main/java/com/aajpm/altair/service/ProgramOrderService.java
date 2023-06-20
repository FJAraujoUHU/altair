package com.aajpm.altair.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.ExposureOrder;
import com.aajpm.altair.entity.Program;
import com.aajpm.altair.entity.ProgramOrder;
import com.aajpm.altair.repository.ProgramOrderRepository;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;
import com.aajpm.altair.utility.Interval;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@Transactional
public class ProgramOrderService extends BasicEntityCRUDService<ProgramOrder> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ProgramOrderRepository programOrderRepository;

    @Override
    protected ProgramOrderRepository getManagedRepository() {
        return programOrderRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    @Autowired
    private AstroObjectService astroObjectService;

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ProgramOrderService() {
        super();
    }

    @Override
    public ProgramOrder create() {
        return new ProgramOrder();
    }

    //////////////////////////////// SAVE METHODS //////////////////////////////

    @Override
    public ProgramOrder save(ProgramOrder order) {
        Assert.notNull(order, "The order cannot be null.");

        Assert.notNull(order.getProgram(), "Program cannot be null.");
        Assert.notEmpty(order.getExposureOrders(), "Must contain at least one exposure order.");

        return super.save(order);
    }

    @Override
    public ProgramOrder update(ProgramOrder order) {
        Assert.notNull(order, "The order cannot be null.");

        Assert.notNull(order.getProgram(), "Program cannot be null.");
        // No need to check for empty exposure orders, since some might have been removed.

        return super.update(order);
    }

    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    /**
     * Finds all {@link ProgramOrder} made by the given user.
     * 
     * @param user The user to find orders for.
     * 
     * @return A {@link Collection} of orders made by the given user.
     */
    public Collection<ProgramOrder> findByUser(AltairUser user) {
        Assert.notNull(user, "The user cannot be null.");

        return programOrderRepository.findByUserId(user.getId());
    }

    /**
     * Finds all {@link ProgramOrder} made by the current user.
     * 
     * @return A {@link Collection} of orders made by the current user.
     */
    public Collection<ProgramOrder> findByCurrentUser() {
        AltairUser currUser = AltairUserService.getCurrentUser();

        Assert.notNull(currUser, "There is no current user.");

        Collection<ProgramOrder> orders = findByUser(currUser);

        Assert.notNull(orders, "The query for all orders by user [" + currUser.getId() + "] returned null.");

        return orders;
    }

    /**
     * Finds all {@link ProgramOrder} that have not been completed.
     * 
     * @return A {@link List} of orders that have not been completed,
     *         ordered by creation time.
     */
    public List<ProgramOrder> findNotCompleted() {
        return programOrderRepository.findByCompletedFalseOrderByCreationTimeAsc();
    }

    /**
     * Finds all {@link ProgramOrder} that have not been completed and are
     * available to be executed in the given time range. That is, orders whose
     * {@link Program} target is visible in the given time range and at least
     * one of the {@link ProgramOrder}'s remaining {@link ExposureOrder} durations
     * fit in the given time range.
     * 
     * @param startTime The start time of the time range.
     * @param endTime  The end time of the time range.
     * 
     * @return A {@link Mono} containing a sorted {@link List} of orders and their
     *         visible intervals that have not been completed and are available
     *         to be executed in the given time range, ordered by the start time
     *         of the visible interval.
     */
    @SuppressWarnings("null")   // The data is loaded from the database, so it cannot be null based on the constraints.
    public Mono<List<Tuple2<ProgramOrder, Interval>>> findInRange(Instant startTime, Instant endTime) {
        Interval range = new Interval(startTime, endTime);
        List<ProgramOrder> pendingOrders = programOrderRepository.findByCompletedFalseOrderByCreationTimeAsc();

        return Flux.fromIterable(pendingOrders)
                // Make a tuple appending the shortest remaining exposure duration to the order.
                .flatMap(order -> astroObjectService.isVisibleInterval(order.getProgram().getTarget(), range)
                        .map(visibleInterval ->  Tuples.of(order, visibleInterval)))
                // Filter out orders that are not visible in the given time range.
                .filter(tuple -> {
                    Collection<ExposureOrder> exposureOrders = tuple.getT1().getExposureOrders();
                    Interval visibleInterval = tuple.getT2();

                    Double shortestRemainingExposure = exposureOrders.stream()
                                .filter(exposure -> !exposure.isCompleted())
                                .map(expOrder -> expOrder.getExposureParams().getExposureTime())
                                .min(Double::compare)
                                .orElse(null);
                    Interval startExposureInterval = visibleInterval != null ? new Interval(visibleInterval.getStart(),
                            visibleInterval.getEnd().minus(Duration.ofSeconds(shortestRemainingExposure.longValue()))) : Interval.empty();


                    return (!startExposureInterval.isEmpty()) && range.contains(startExposureInterval);
                })
                // Collect all tuples into a list, sorted by the start time of the visible interval.
                .collectSortedList((t1, t2) -> t1.getT2().getStart().compareTo(t2.getT2().getStart()));
    }

    //#endregion Methods
    
}
