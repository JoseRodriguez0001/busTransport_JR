package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.KpiDtos.KpiCreateRequest;
import com.unimag.bustransport.api.dto.KpiDtos.KpiResponse;
import com.unimag.bustransport.api.dto.KpiDtos.KpiUpdateRequest;
import com.unimag.bustransport.services.KpiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kpis")
@RequiredArgsConstructor
@Validated
public class KpiController {

    private final KpiService service;

    @PostMapping
    public ResponseEntity<KpiResponse> create(@Valid @RequestBody KpiCreateRequest req,
                                              UriComponentsBuilder uriBuilder) {
        var kpiCreated = service.createKpi(req);
        var location = uriBuilder.path("/api/v1/kpis/{id}")
                .buildAndExpand(kpiCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(kpiCreated);
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<KpiResponse> getByName(@PathVariable String name) {
        return ResponseEntity.ok(service.getKpiByName(name));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<KpiResponse>> getRecent() {
        return ResponseEntity.ok(service.getKpiByRecentMetrics());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id,
                                       @Valid @RequestBody KpiUpdateRequest req) {
        service.updateKpi(id, req);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteKpi(id);
        return ResponseEntity.noContent().build();
    }
}