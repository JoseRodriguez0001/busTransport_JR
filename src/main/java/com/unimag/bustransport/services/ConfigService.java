package com.unimag.bustransport.services;

import com.unimag.bustransport.api.dto.ConfigDtos;

import java.math.BigDecimal;
import java.util.List;

public interface ConfigService {
    ConfigDtos.ConfigResponse createConfig(ConfigDtos.ConfigCreateRequest request);

    void updateConfig(Long id, ConfigDtos.ConfigUpdateRequest request);

    void deleteConfig(Long id);

    List<ConfigDtos.ConfigResponse> getAllConfigs();

    ConfigDtos.ConfigResponse getConfigByKey(String key);


    BigDecimal getValueAsBigDecimal(String key);

    Integer getValueAsInt(String key);

    String getValueAsString(String key);
}
