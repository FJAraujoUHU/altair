package com.aajpm.altair.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.ProgramOrder;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface ProgramOrderRepository extends JpaRepository<ProgramOrder, Long> {

    Collection<ProgramOrder> findByUserId(long userID);

    Collection<ProgramOrder> findByCompletedFalse();

    List<ProgramOrder> findByCompletedFalseOrderByCreationTimeAsc();

    Collection<ProgramOrder> findByProgramTargetId(long astroObjectID);
    
}
