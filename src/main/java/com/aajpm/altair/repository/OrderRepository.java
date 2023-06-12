package com.aajpm.altair.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.Order;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Collection<Order> findByUserId(long userID);

    Collection<Order> findByCompletedFalse();

    List<Order> findByCompletedFalseOrderByCreationTimeAsc();
    
}
