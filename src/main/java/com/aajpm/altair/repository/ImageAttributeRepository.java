package com.aajpm.altair.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.ImageAttribute;

@Repository
public interface ImageAttributeRepository extends JpaRepository<ImageAttribute, Long> {

    ImageAttribute findByName(String name);
    
}
