package com.aajpm.altair.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.ControlOrder;
import com.aajpm.altair.entity.ExposureOrder;
import com.aajpm.altair.entity.Order;
import com.aajpm.altair.entity.ProgramOrder;
import com.aajpm.altair.repository.OrderRepository;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;
import com.aajpm.altair.utility.Interval;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@Transactional
public class OrderService {

    /////////////////////////// MANAGED REPOSITORY ////////////////////////////

    @Autowired
    private OrderRepository orderRepository;

    /////////////////////////// SUPPORTING SERVICES ///////////////////////////

    @Autowired
    private ControlOrderService controlOrderService;

    @Autowired
    private ProgramOrderService programOrderService;

    /////////////////////////////// CONSTRUCTORS //////////////////////////////

    public OrderService() {
        super();
    }

    //////////////////////////////// BASIC CRUD ///////////////////////////////
    //#region CRUD

    public Collection<Order> findAll() {
        Collection<Order> orders = orderRepository.findAll();

        // To check for transaction integrity
        Assert.notNull(orders, "The query for all orders returned null.");

        return orders;
    }

    public Order findById(long id) {
        Assert.isTrue(id != 0, "The id of the query [" + id + "] is not valid.");
        Order order = orderRepository.findById(id).orElse(null);

        // To check for transaction integrity
        Assert.notNull(order, "The query for order with id " + id + " returned null.");

        return order;
    }

    public Order save(Order order) {
        Assert.notNull(order, "The order to save cannot be null.");
        Assert.isTrue(order.getId() == 0, "This object has already been saved.");

        return orderRepository.save(order);
    }

    public Order update(Order order) {
        Assert.notNull(order, "The order to update cannot be null.");
        Assert.isTrue(order.getId() != 0, "This object has not been saved.");
        Assert.isTrue(orderRepository.existsById(order.getId()), "The order to update does not exist.");

        return orderRepository.save(order);
    }

    public void delete(Order order) {
        Assert.notNull(order, "The order to delete cannot be null.");
        Assert.isTrue(order.getId() != 0, "This object has not been saved.");
        Assert.isTrue(orderRepository.existsById(order.getId()), "The order to delete does not exist.");

        orderRepository.delete(order);
    }

    //#endregion
    ///////////////////////////////// METHODS /////////////////////////////////
    //#region METHODS

    public Collection<Order> findByUser(AltairUser user) {
        Assert.notNull(user, "The user cannot be null.");

        return orderRepository.findByUserId(user.getId());
    }

    public Collection<Order> findByCurrentUser() {
        AltairUser currUser = AltairUserService.getCurrentUser();

        Assert.notNull(currUser, "There is no current user.");

        Collection<Order> orders = findByUser(currUser);

        Assert.notNull(orders, "The query for all orders by user [" + currUser.getId() + "] returned null.");

        return orders;
    }

    public List<Order> findNotCompleted() {
        return orderRepository.findByCompletedFalseOrderByCreationTimeAsc();
    }

    public void buildSchedule(Interval interval) {
        buildSchedule(interval.getStart(), interval.getEnd());
    }
    
    /**
     * Builds a schedule of orders between the specified start and end times.
     * The schedule is built by fetching the requested {@link ControlOrder}s
     * and filling the spaces between them with {@link ProgramOrder}s.
     * 
     * @param startTime the start time of the schedule.
     * @param endTime the end time of the schedule.
     * 
     * @return a {@link Mono} emitting a list of orders representing the schedule.
     */
    public Mono<List<Order>> buildSchedule(Instant startTime, Instant endTime) {
        
        List<Order> schedule = new LinkedList<>();
        List<ControlOrder> controlOrders = controlOrderService.findInRangeOpen(startTime, endTime);
        List<Interval> freeSlots = controlOrderService.findAvailableTime(startTime, endTime);

        boolean startsOnFreeSlot = !freeSlots.isEmpty() && !freeSlots.get(0).getStart().isBefore(startTime);

        if (!startsOnFreeSlot) {    // If it starts on a controlOrder, set it as the first scheduled order.
            schedule.add(controlOrders.remove(0));
        }

        return Flux.fromIterable(freeSlots)
                .flatMap(slot -> programOrderService.findInRange(slot.getStart(), slot.getEnd())
                    .map(programOrders -> {
                        // Replace the free time slot with a schedule of ProgramOrders that fit in it.
                        List<ProgramOrder> slotSchedule = new LinkedList<>();

                        Instant timeCursor = slot.getStart();
                        Instant slotEnd = slot.getEnd();

                        // While there are ProgramOrder intervals that fit between timeCursor and slotEnd,
                        // find the first one that fits and add it to the schedule.

                        boolean isSlotFilled = false;
                        do {
                            Tuple2<ProgramOrder, Interval> nextOrder = selectNextOrder(programOrders, schedule, timeCursor, slotEnd);
                            if (nextOrder == null) break; // If there are no more ProgramOrders, end the loop.
                            // Add the order to the schedules.
                            slotSchedule.add(nextOrder.getT1());
                            schedule.add(nextOrder.getT1());
                            timeCursor = nextOrder.getT2().getEnd();
                            isSlotFilled = !timeCursor.isBefore(slotEnd);
                        } while (!isSlotFilled);

                        if (!controlOrders.isEmpty())
                            // Add the next ControlOrder to the schedule.
                            schedule.add(controlOrders.remove(0));
                        
                        return slotSchedule;
                    })
                )
                // Return the schedule with the remaining ControlOrders appended, if any.
                .then(Mono.just(append(schedule, controlOrders)));  
    }

    /**
     * Appends a list to another. The original list is modified.
     * 
     * @param <T> The type of the elements in the list.
     * @param list The list to append to.
     * @param toAppend The list to append.
     * @return The list with the appended elements.
     */
    private static <T> List<T> append(List<T> list, List<? extends T> toAppend) {
        list.addAll(toAppend);
        return list;
    }

    /**
     * Selects the next {@link ProgramOrder} to be scheduled. It will select the
     * first ProgramOrder that fits between the timeCursor and the slotEnd with 
     * unqueued {@link ExposureOrder}.
     * 
     * @param programOrders The list of tuples to select from.
     * @param schedule The current schedule/queued orders.
     * @param timeCursor The current time cursor, that is, the time of the last scheduled order.
     * @param slotEnd The end of the current free time slot.
     * @return A tuple with the selected {@link ProgramOrder} and the interval it fits in,
     *         or {@code null} if there are no more ProgramOrders to select.
     */
    private Tuple2<ProgramOrder, Interval> selectNextOrder(List<Tuple2<ProgramOrder, Interval>> programOrders, List<Order> schedule, Instant timeCursor, Instant slotEnd) {

        // FIFO approach, maybe add a algorithm select in a future release.
        for (Tuple2<ProgramOrder, Interval> tuple : programOrders) {
            ProgramOrder pOrder = tuple.getT1();
            List<ExposureOrder> exposures = pOrder.getExposureOrders()
                                                    .stream()
                                                    .filter(exp -> !exp.isCompleted())
                                                    .sorted((e1, e2) -> e1.getExposureParams()
                                                                        .getExposureTime()
                                                                        .compareTo(
                                                                        e2.getExposureParams()
                                                                        .getExposureTime())
                                                            )
                                                    .toList();
            
            int remainingExposures = exposures.size();
            int timesAlreadyPresent = (int) schedule.stream()
                                                .filter(ProgramOrder.class::isInstance)
                                                .map(ProgramOrder.class::cast)
                                                .filter(order -> order.equals(pOrder))
                                                .count();

            
            if (remainingExposures > timesAlreadyPresent) {
                // If there are unqueued exposures, return the first one unless it doesn't fit in the slot.
                double exposureSeconds = exposures.get(timesAlreadyPresent).getExposureParams().getExposureTime();
                Duration exposureDuration = Duration.ofSeconds((long) exposureSeconds);
                Duration remainingTime = Duration.between(timeCursor, slotEnd);

                if (remainingTime.compareTo(exposureDuration) >= 0) // If the exposure fits in the slot, return it.
                    return Tuples.of(pOrder, new Interval(timeCursor, exposureDuration)); 
            }                              
        }

        // If there are no valid ProgramOrders, return null.
        return null;
    }

    //#endregion
}
