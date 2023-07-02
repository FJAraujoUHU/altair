package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.AstroObject;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface AstroObjectRepository extends JpaRepository<AstroObject, Long> {
    
    AstroObject findByName(String name);

    AstroObject findByNameIgnoreCase(String name);

    Collection<AstroObject> findByType(String type);

    Collection<AstroObject> findByBaseFocus(int baseFocus);

}
