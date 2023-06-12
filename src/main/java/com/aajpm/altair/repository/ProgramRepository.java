package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.Program;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface ProgramRepository extends JpaRepository<Program, Long> {

    Program findByName(String name);

    Collection<Program> findByEnabled(boolean enabled);

    Collection<Program> findByTargetId(long targetID);
    
}
