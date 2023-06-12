package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Collection<Order> findByUserID(long userID);

    Collection<Order> findByCompletedFalse();
    
}
