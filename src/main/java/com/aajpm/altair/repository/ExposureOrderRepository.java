package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.aajpm.altair.entity.ExposureOrder;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface ExposureOrderRepository extends JpaRepository<ExposureOrder, Long> {

    Collection<ExposureOrder> findByProgramUserId(long userID);

    Collection<ExposureOrder> findByProgramId(long programID);

    Collection<ExposureOrder> findByExposureParamsId(long exposureParams);

    Collection<ExposureOrder> findByState(String state);

    @Query("SELECT eo FROM ExposureOrder eo WHERE eo.state  <> 'COMPLETED'")
    Collection<ExposureOrder> findNotCompleted();
    
}
