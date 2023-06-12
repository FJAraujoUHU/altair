package com.aajpm.altair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.AstroObject;

@Repository
public interface AstroObjectRepository extends JpaRepository<AstroObject, Long> {
    
    AstroObject findByName(String name);

}
