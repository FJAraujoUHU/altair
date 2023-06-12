package com.aajpm.altair.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.aajpm.altair.entity.AstroImage;

@Repository
public interface AstroImageRepository extends JpaRepository<AstroImage, Long> {

    @Query("SELECT i FROM AstroImage i WHERE i.controlOrder.user.id = ?1 OR i.exposureOrder.user.id = ?1")    
    Collection<AstroImage> findByUserID(long userID);

    Collection<AstroImage> findByTargetId(long targetId);

    Collection<AstroImage> findByValuesValueAndValuesAttributeName(String imageValue, String attributeName);

    List<AstroImage> findByCreationDateBetweenOrderedByCreationDateAsc(Instant start, Instant end);

    List<AstroImage> findByCreationDateBetweenAndTargetIdOrderByCreationDateAsc(Instant start, Instant end, long targetId);

}
