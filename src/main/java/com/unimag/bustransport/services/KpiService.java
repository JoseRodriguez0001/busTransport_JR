package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.KpiDtos;

import java.util.List;

public interface KpiService {
    KpiDtos.KpiResponse createKpi(KpiDtos.KpiCreateRequest request);
    void updateKpi(Long kpiId,KpiDtos.KpiUpdateRequest request);
    void deleteKpi(Long kpiId);
    KpiDtos.KpiResponse getKpiByName(String kpiName);
    List<KpiDtos.KpiResponse> getKpiByRecentMetrics();
}
