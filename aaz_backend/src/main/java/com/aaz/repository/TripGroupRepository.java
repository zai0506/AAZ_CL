package com.aaz.repository;

import com.aaz.entity.TripGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TripGroupRepository extends JpaRepository<TripGroup, Long> {
    Optional<TripGroup> findByTripCode(String tripCode);
    
    @Query("SELECT DISTINCT t FROM TripGroup t JOIN t.members m WHERE m.user.id = :userId")
    List<TripGroup> findByUserId(Long userId);
    
    Boolean existsByTripCode(String tripCode);
}