package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.ConfigDtos;
import com.unimag.bustransport.domain.entities.Config;
import com.unimag.bustransport.domain.repositories.ConfigRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.ConfigService;
import com.unimag.bustransport.services.mapper.ConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConfigServiceImpl implements ConfigService {
    private final ConfigRepository configRepository;
    private final ConfigMapper configMapper;

    @Override
    public ConfigDtos.ConfigResponse createConfig(ConfigDtos.ConfigCreateRequest request) {
        if (configRepository.existsByKey(request.key())) {
            throw new IllegalStateException("Configuration with key already exists: " + request.key());
        }
        Config config = configMapper.toEntity(request);
        configRepository.save(config);
        log.info("Config created with key: {}", config.getKey());
        return configMapper.toResponse(config);
    }

    @Override
    public void updateConfig(Long id, ConfigDtos.ConfigUpdateRequest request) {
        Config config = configRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Config with ID %d not found", id)));

        if (request.value() != null) {
            config.setValue(request.value());
        }

        configRepository.save(config);
        log.info("Config with ID {} updated", id);
    }

    @Override
    public void deleteConfig(Long id) {
        Config config = configRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Config with ID %d not found", id)));
        configRepository.delete(config);
        log.info("Config with ID {} deleted", id);
    }

    @Override
    public List<ConfigDtos.ConfigResponse> getAllConfigs() {
        return configRepository.findAll().stream().map(configMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ConfigDtos.ConfigResponse getConfigByKey(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException(String.format("Config with key '%s' not found", key)));
        return configMapper.toResponse(config);
    }

    @Override
    public BigDecimal getValueAsBigDecimal(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException(String.format("Config with key '%s' not found", key)));
        try {
            return new BigDecimal(config.getValue());
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("Value of '%s' is not a valid number", key));
        }
    }

    @Override
    public Integer getValueAsInt(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException(String.format("Config with key '%s' not found", key)));
        try {
            return Integer.parseInt(config.getValue());
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("Value of '%s' is not a valid integer", key));
        }
    }

    @Override
    public String getValueAsString(String key) {
        Config config = configRepository.findByKey(key)
                .orElseThrow(() -> new NotFoundException(String.format("Config with key '%s' not found", key)));
        return config.getValue();
    }
}
