package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Kpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KpiRepository extends JpaRepository<Kpi, Long> {
    Optional<Kpi> findByName(String name);

    @Query("SELECT k FROM Kpi k ORDER BY k.calculatedAt DESC")
    List<Kpi> findRecentMetrics();
}
