package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.Program;

@Repository // Dismiss the error in the IDE, it do be trippin
public interface ProgramRepository extends JpaRepository<Program, Long> {

    Program findByName(String name);

    Collection<Program> findByEnabled(boolean enabled);

    Collection<Program> findByTargetID(long targetID);
    
}
