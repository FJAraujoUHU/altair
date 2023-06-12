package com.aajpm.altair.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.ControlOrder;

@Repository
public interface ControlOrderRepository extends JpaRepository<ControlOrder, Long>{

    Collection<ControlOrder> findByUserID(long userID);

    Collection<ControlOrder> findByCompletedFalse();

    List<ControlOrder> findByCompletedFalseOrderedByCreationTime();

    @Query("SELECT o FROM ControlOrder o WHERE o.requestedTime >= ?1 AND o.requestedTime < ?2")
    Collection<ControlOrder> findStartsInRange(Instant startTime, Instant endTime);

    @Query("SELECT o FROM ControlOrder o WHERE o.requestedTime >= ?1 AND o.requestedTime < ?2 ORDER BY o.requestedTime ASC")
    List<ControlOrder> findStartsInRangeOrdered(Instant startTime, Instant endTime);
    
}
