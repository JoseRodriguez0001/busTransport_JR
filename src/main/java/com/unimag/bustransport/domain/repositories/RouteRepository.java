package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route,Long> {
    Optional<Route> findByCode(String code);

    List<Route> findByOriginAndDestination(String origin, String destination);

    List<Route> findByOrigin(String origin);

    List<Route> findByDestination(String destination);
    @Query( "SELECT r " +
            "FROM Route r " +
            "WHERE r.origin = :origin OR r.destination = :destination")
    List<Route> findByOriginOrDestination(@Param("origin") String origin, @Param("destination") String destination);

    boolean existsByCode(String code);
}
