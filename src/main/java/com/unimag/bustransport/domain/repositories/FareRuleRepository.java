package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.FareRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FareRuleRepository extends JpaRepository<FareRule,Long> {
    Optional<FareRule> findByRouteIdAndFromStopIdAndToStopId(Long routeId, Long fromStopId, Long toStopId);
    List<FareRule> findByRouteId(Long routeId);
}
