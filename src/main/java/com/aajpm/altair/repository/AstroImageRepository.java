package com.aajpm.altair.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.aajpm.altair.entity.AstroImage;

//@Repository - not needed because this is a subinterface of JpaRepository
public interface AstroImageRepository extends JpaRepository<AstroImage, Long> {

    @Query("SELECT i FROM AstroImage i WHERE i.controlOrder.user.id = ?1 OR i.exposureOrder.program.user.id = ?1")    
    Collection<AstroImage> findByUserId(long userId);

    Collection<AstroImage> findByTargetId(long targetId);

    Collection<AstroImage> findByValuesValueAndValuesAttributeName(String imageValue, String attributeName);

    //Check if it compiles
    Collection<AstroImage> findByValuesAttributeNameAndValuesValueIsNull(String attributeName);

    @Query("SELECT i FROM AstroImage i WHERE i.creationDate BETWEEN ?1 AND ?2 AND (i.controlOrder.user.id = ?3 OR i.exposureOrder.program.user.id = ?3) AND i.target.id = ?4 ORDER BY i.creationDate ASC")
    List<AstroImage> findByCreationDateBetweenAndUserIdAndTargetIdOrderByCreationDateAsc(Instant start, Instant end, long userId, long targetId);

    List<AstroImage> findByCreationDateBetweenOrderByCreationDateAsc(Instant start, Instant end);

    List<AstroImage> findByCreationDateBetweenAndTargetIdOrderByCreationDateAsc(Instant start, Instant end, long targetId);

}
