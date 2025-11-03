package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.FareRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FareRuleRepository extends JpaRepository<FareRule,Long> {
    Optional<FareRule> finByRouteIdAndFromStopIdAndToStopId(Long routeId, Long fromStopId, Long toStopId);
    List<FareRule> findByRouteId(Long routeId);
    //Encontrar tarifas que cubren un tramo
    @Query("SELECT fr " +
            "FROM FareRule fr " +
            "WHERE fr.route.id = :routeId " +
            "      AND fr.fromStop.order <= :fromOrder " +
            "      AND fr.toStop.order >= :toOrder")
    List<FareRule> findApplicableFares(@Param("routeId") Long routeId, @Param("fromOrder") Integer fromOrder, @Param("toOrder") Integer toOrder);
    List<FareRule> findByRouteIdAndDinamyPricing(Long routeId, FareRule.dinamyPricing dinamyPricing);
}
