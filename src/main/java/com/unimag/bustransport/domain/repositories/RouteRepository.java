package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route,Long> {

    List<Route> findByOriginAndDestination(String origin, String destination);

    List<Route> findByOrigin(String origin);

    List<Route> findByDestination(String destination);

    boolean existsByCode(String code);
}
