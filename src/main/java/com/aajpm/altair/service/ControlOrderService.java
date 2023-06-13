package com.aajpm.altair.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.ControlOrder;
import com.aajpm.altair.repository.ControlOrderRepository;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;
import com.aajpm.altair.utility.Interval;

@Service
@Transactional
public class ControlOrderService extends BasicEntityCRUDService<ControlOrder> {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private ControlOrderRepository controlOrderRepository;

    @Override
    protected ControlOrderRepository getManagedRepository() {
        return controlOrderRepository;
    }

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    // None

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public ControlOrderService() {
        super();
    }

    @Override
    public ControlOrder create() {
        return new ControlOrder();
    }

    ///////////////////////////////// METHODS /////////////////////////////////
    //#region Methods

    /**
     * Finds all {@link ControlOrder} made by the given user.
     * 
     * @param user The user to find orders for.
     * 
     * @return A {@link Collection} of orders made by the given user.
     */
    public Collection<ControlOrder> findByUser(AltairUser user) {
        Assert.notNull(user, "The user cannot be null.");

        return controlOrderRepository.findByUserId(user.getId());
    }

    /**
     * Finds all {@link ControlOrder} made by the current user.
     * 
     * @return A {@link Collection} of orders made by the current user.
     */
    public Collection<ControlOrder> findByCurrentUser() {
        AltairUser currUser = AltairUserService.getCurrentUser();

        Assert.notNull(currUser, "There is no current user.");

        Collection<ControlOrder> orders = findByUser(currUser);

        Assert.notNull(orders, "The query for all orders by user [" + currUser.getId() + "] returned null.");

        return orders;
    }

    /**
     * Finds all {@link ControlOrder} that have not been completed.
     * 
     * @return A {@link List} of orders that have not been completed, ordered by
     *         creation time.
     */
    public List<ControlOrder> findNotCompleted() {
        return controlOrderRepository.findByCompletedFalseOrderByCreationTimeAsc();
    }

    /**
     * Finds all {@link ControlOrder} scheduled in the given interval.
     * 
     * @param interval The interval to find orders in.
     * 
     * @return A {@link List} of orders that are requested to be scheduled in the
     *         given interval, ordered by the start of the requested time.
     */
    public List<ControlOrder> findInRange(Interval interval) {
        return findInRange(interval.getStart(), interval.getEnd());
    } 

    /**
     * Finds all {@link ControlOrder} scheduled in the given interval.
     * 
     * @param startTime The start of the interval to find orders in.
     * @param endTime   The end of the interval to find orders in.
     * 
     * @return A {@link List} of orders that are requested to be scheduled in the
     *         given interval, ordered by the start of the requested time.
     */
    public List<ControlOrder> findInRange(Instant startTime, Instant endTime) {
        List<ControlOrder> orders = controlOrderRepository.findStartsInRangeOrder(startTime, endTime);
        orders = orders.stream()
                .filter(order -> order
                    .getRequestedTime()
                    .plus(order.getRequestedDuration())
                    .isBefore(endTime))
                .sorted((o1, o2) -> o1.getRequestedTime().compareTo(o2.getRequestedTime()))
                .toList();

        return orders;
    }

    /**
     * Finds all time slots that are free in the given interval, that is, the
     * intervals that are not taken by any {@link ControlOrder}.
     * 
     * @param interval The interval to find available time in.
     * 
     * @return A {@link List} of intervals that are free in the given interval.
     */
    public List<Interval> findAvailableTime(Interval interval) {
        return findAvailableTime(interval.getStart(), interval.getEnd());
    }
    
    /**
     * Finds all time slots that are free in the given interval, that is, the
     * intervals that are not taken by any {@link ControlOrder}.
     * 
     * @param startTime The start of the interval to find available time in.
     * @param endTime   The end of the interval to find available time in.
     * 
     * @return A {@link List} of intervals that are free in the given interval.
     */
    public List<Interval> findAvailableTime(Instant startTime, Instant endTime) {
        List<ControlOrder> orders = findInRange(startTime, endTime);

        // If there are no orders, then the entire interval is available
        if (orders.isEmpty()) {
            return List.of(new Interval(startTime, endTime));
        }

        // Convert the orders to intervals
        List<Interval> taken = orders.stream()
                .map(order -> new Interval(order.getRequestedTime(), order.getRequestedDuration()))
                .sorted((o1, o2) -> o1.getStart().compareTo(o2.getStart()))
                .toList();
        
        // Find the gaps between the intervals, which are the available times
        List<Interval> available = new ArrayList<>();

        if (startTime.isBefore(taken.get(0).getStart())) {
            available.add(new Interval(startTime, taken.get(0).getStart()));
        }
        for (int i = 0; i < taken.size() - 1; i++) {
            Interval curr = taken.get(i);
            Interval next = taken.get(i + 1);
            if (curr.getEnd().isBefore(next.getStart())) {
                available.add(new Interval(curr.getEnd(), next.getStart()));
            }
        }
        if (taken.get(taken.size() - 1).getEnd().isBefore(endTime)) {
            available.add(new Interval(taken.get(taken.size() - 1).getEnd(), endTime));
        }

        return available;
    }

    //#endregion

}
