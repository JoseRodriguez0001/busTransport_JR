package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    Optional<Assignment> findByTripId(Long tripId);
    @Query("SELECT a FROM Assignment a WHERE a.driver.id = :driverId")
    List<Assignment> findByDriver(@Param("driverId") Long driverId);
}
