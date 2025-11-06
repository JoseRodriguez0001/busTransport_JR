package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.KpiDtos;
import com.unimag.bustransport.domain.entities.Kpi;
import com.unimag.bustransport.domain.repositories.KpiRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.KpiService;
import com.unimag.bustransport.services.mapper.KpiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class KpiServiceImpl implements KpiService {
    private final KpiRepository repository;
    private final KpiMapper mapper;
    @Override
    public KpiDtos.KpiResponse createKpi(KpiDtos.KpiCreateRequest request) {
        Kpi kpi = mapper.toEntity(request);
        Kpi kpiSaved = repository.save(kpi);
        log.info("Kpi created successfully");
        return mapper.toResponse(kpiSaved);
    }

    @Override
    public void updateKpi(Long kpiId,KpiDtos.KpiUpdateRequest request) {
        Kpi kpi= repository.findById(kpiId)
                .orElseThrow(()-> new NotFoundException("Kpi not found"));
        mapper.updateEntityFromRequest(request,kpi);
        repository.save(kpi);
        log.info("Kpi updated successfully");
    }

    @Override
    public void deleteKpi(Long kpiId) {
        Kpi kpi = repository.findById(kpiId)
                .orElseThrow(()-> new NotFoundException("Kpi not found"));
        repository.delete(kpi);
        log.info("Kpi deleted successfully");
    }

    @Override
    public KpiDtos.KpiResponse getKpiByName(String kpiName) {
        Kpi kpi = repository.findByName(kpiName)
                .orElseThrow(()-> new NotFoundException("Kpi not found"));
        return mapper.toResponse(kpi);
    }

    @Override
    public List<KpiDtos.KpiResponse> getKpiByRecentMetrics() {
        return repository.findRecentMetrics().stream().map(mapper::toResponse).toList();
    }
}
