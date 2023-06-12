package com.aajpm.altair.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.ProgramOrder;

@Repository
public interface ProgramOrderRepository extends JpaRepository<ProgramOrder, Long> {

    Collection<ProgramOrder> findByUserID(long userID);

    Collection<ProgramOrder> findByCompletedFalse();

    List<ProgramOrder> findByCompletedFalseOrderedByCreationTime();

    Collection<ProgramOrder> findByProgramTargetID(long astroObjectID);
    
}
