package com.aajpm.altair.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.ExposureParams;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface ExposureParamsRepository extends JpaRepository<ExposureParams, Long> {

    List<ExposureParams> findByProgramIdOrderByExposureTimeAsc(long programID);

    Collection<ExposureParams> findByFilter(String filter);

    List<ExposureParams> findByExposureTimeLessThanOrderByExposureTimeAsc(double exposureSeconds);

}
