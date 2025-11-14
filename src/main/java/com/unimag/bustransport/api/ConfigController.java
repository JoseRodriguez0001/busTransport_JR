package com.unimag.bustransport.api;

import com.unimag.bustransport.api.dto.ConfigDtos;
import com.unimag.bustransport.services.ConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@Validated
public class ConfigController {

    private final ConfigService configService;

    @PostMapping
    public ResponseEntity<ConfigDtos.ConfigResponse> create(
            @Valid @RequestBody ConfigDtos.ConfigCreateRequest request,
            UriComponentsBuilder uriBuilder) {
        var configCreated = configService.createConfig(request);
        var location = uriBuilder.path("/api/v1/configs/{id}")
                .buildAndExpand(configCreated.id())
                .toUri();
        return ResponseEntity.created(location).body(configCreated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @Valid @RequestBody ConfigDtos.ConfigUpdateRequest request) {
        configService.updateConfig(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        configService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ConfigDtos.ConfigResponse>> getAll() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }

    @GetMapping("/by-key/{key}")
    public ResponseEntity<ConfigDtos.ConfigResponse> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(configService.getConfigByKey(key));
    }
}