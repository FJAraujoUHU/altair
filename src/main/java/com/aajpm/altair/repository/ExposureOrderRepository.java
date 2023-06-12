package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.ExposureOrder;

@Repository
public interface ExposureOrderRepository extends JpaRepository<ExposureOrder, Long> {

    Collection<ExposureOrder> findByUserID(long userID);

    Collection<ExposureOrder> findByProgramID(long programID);

    Collection<ExposureOrder> findByExposureParamsID(long exposureParams);

    Collection<ExposureOrder> findByCompletedFalse();
    
}
