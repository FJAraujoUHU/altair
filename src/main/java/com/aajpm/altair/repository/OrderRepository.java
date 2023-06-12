package com.aajpm.altair.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Collection<Order> findByUserID(long userID);

    Collection<Order> findByCompletedFalse();

    List<Order> findByCompletedFalseOrderedByCreationTime();
    
}
