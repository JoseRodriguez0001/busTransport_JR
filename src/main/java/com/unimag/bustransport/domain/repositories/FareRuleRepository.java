package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.FareRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FareRuleRepository extends JpaRepository<FareRule,Long> {
    Optional<FareRule> findByRouteIdAndFromStopIdAndToStopId(Long routeId, Long fromStopId, Long toStopId);
    List<FareRule> findByRouteId(Long routeId);
}
