package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.ImageValue;

@Repository
public interface ImageValueRepository extends JpaRepository<ImageValue, Long> {
    
    ImageValue findByImageIDAndAttributeID(long imageID, long attributeID);

    Collection<ImageValue> findByAttributeID(long attributeID);

    Collection<ImageValue> findByImageID(long imageID);
    
}
