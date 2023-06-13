package com.aajpm.altair.service;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.aajpm.altair.entity.Order;
import com.aajpm.altair.repository.OrderRepository;
import com.aajpm.altair.security.account.AltairUser;
import com.aajpm.altair.security.account.AltairUserService;

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

    // TODO: make a query that gets all available orders for a given interval, that basically
    // asks each subtype to do it and then merge the results.
    /**
     * Gets a time interval this order is available to be run inside of the given interval.
     * @param interval The interval to check for availability.
     * @return A time interval this order is available to be run inside of the given interval, or null if none available.
     */
    //public abstract Interval getAvailableTime(Interval interval);

    /**
     * Gets a time interval this order is available to be run, first available.
     * @return A time interval this order is available to be run, first available, or null if none available.
     */
    //public abstract Interval getAvailableTime();

    //#endregion
}
