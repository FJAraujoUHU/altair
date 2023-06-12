package com.aajpm.altair.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.ImageAttribute;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface ImageAttributeRepository extends JpaRepository<ImageAttribute, Long> {

    ImageAttribute findByName(String name);
    
}
