package com.aajpm.altair.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.ExposureParams;

@Repository
public interface ExposureParamsRepository extends JpaRepository<ExposureParams, Long> {

    List<ExposureParams> findByProgramIDOrderedByExposureTime(long programID);
    
}
