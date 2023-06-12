package com.aajpm.altair.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aajpm.altair.entity.ImageValue;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface ImageValueRepository extends JpaRepository<ImageValue, Long> {
    
    ImageValue findByImageIdAndAttributeId(long imageID, long attributeID);

    Collection<ImageValue> findByAttributeId(long attributeID);

    Collection<ImageValue> findByImageId(long imageID);
    
}
