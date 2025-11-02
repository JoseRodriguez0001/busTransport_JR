package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByEntityType(Incident.EntityType entityType);

    @Query("SELECT i FROM Incident i WHERE i.type = :type ORDER BY i.createdAt DESC")
    List<Incident> findRecentByType(@Param("type") Incident.Type type);
}
