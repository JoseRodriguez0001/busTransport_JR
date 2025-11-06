package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.ConfigDtos;
import com.unimag.bustransport.domain.entities.Config;
import com.unimag.bustransport.domain.repositories.ConfigRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ConfigServiceImpl implements ConfigService {
    private final ConfigRepository configRepository;
    private final ConfigMapper configMapper;

    @Override
    public ConfigDtos.ConfigResponse createConfig(ConfigDtos.ConfigCreateRequest request) {
        if (configRepository.existsByKey(request.key())) {
            throw new IllegalStateException("Ya existe una configuración con la clave: " + request.key());
        }
        Config config = configMapper.toEntity(request);
        configRepository.save(config);
        return configMapper.toResponse(config);
    }

    @Override
    public void updateConfig(Long id, ConfigDtos.ConfigUpdateRequest request) {
        Config config = configRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Configuración no encontrada"));

        if (request.value() != null) {
            config.setValue(request.value());
        }

        configRepository.save(config);
    }

    @Override
    public void deleteConfig(Long id) {
        Config config = configRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Configuración no encontrada"));
        configRepository.delete(config);
    }

    @Override
    public List<ConfigDtos.ConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream().map(configMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ConfigDtos.ConfigResponse getConfigByKey(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Configuración con clave '" + key + "' no encontrada"));
        return configMapper.toResponse(config);
    }

    @Override
    public BigDecimal getValueAsBigDecimal(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Configuración con clave '" + key + "' no encontrada"));
        try {
            return new BigDecimal(config.getValue());
        } catch (NumberFormatException e) {
            throw new RuntimeException("El valor de '" + key + "' no es un número válido");
        }
    }

    @Override
    public Integer getValueAsInt(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Configuración con clave '" + key + "' no encontrada"));
        try {
            return Integer.parseInt(config.getValue());
        } catch (NumberFormatException e) {
            throw new RuntimeException("El valor de '" + key + "' no es un número entero válido");
        }
    }

    @Override
    public String getValueAsString(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException("Configuración con clave '" + key + "' no encontrada"));
        return config.getValue();
    }
}
