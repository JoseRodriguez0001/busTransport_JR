package com.unimag.bustransport.services.impl;

import com.unimag.bustransport.api.dto.ConfigDtos;
import com.unimag.bustransport.domain.entities.Config;
import com.unimag.bustransport.domain.repositories.ConfigRepository;
import com.unimag.bustransport.exception.NotFoundException;
import com.unimag.bustransport.services.mapper.ConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfigServiceImplTest {

    @Mock
    private ConfigRepository configRepository;

    @Spy
    private final ConfigMapper configMapper = Mappers.getMapper(ConfigMapper.class);

    @InjectMocks
    private ConfigServiceImpl configService;


    private Config givenConfig(String key, String value) {
        return Config.builder()
                .id(1L)
                .key(key)
                .value(value)
                .build();
    }

    private ConfigDtos.ConfigCreateRequest givenCreateRequest(String key, String value) {
        return new ConfigDtos.ConfigCreateRequest(key, value);
    }

    private ConfigDtos.ConfigUpdateRequest givenUpdateRequest(String value) {
        return new ConfigDtos.ConfigUpdateRequest(value);
    }

    @Test
    @DisplayName("Debe crear config correctamente")
    void shouldCreateConfig() {
        // Given
        ConfigDtos.ConfigCreateRequest request = givenCreateRequest("MAX_SEATS", "40");
        Config savedConfig = givenConfig("MAX_SEATS", "40");

        when(configRepository.existsByKey("MAX_SEATS")).thenReturn(false);
        when(configRepository.save(any(Config.class))).thenReturn(savedConfig);

        // When
        ConfigDtos.ConfigResponse response = configService.createConfig(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.key()).isEqualTo("MAX_SEATS");
        assertThat(response.value()).isEqualTo("40");

        verify(configRepository, times(1)).existsByKey("MAX_SEATS");
        verify(configRepository, times(1)).save(any(Config.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la clave ya existe")
    void shouldThrowExceptionWhenKeyAlreadyExists() {
        // Given
        ConfigDtos.ConfigCreateRequest request = givenCreateRequest("MAX_SEATS", "40");

        when(configRepository.existsByKey("MAX_SEATS")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> configService.createConfig(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe una configuración con la clave: MAX_SEATS");

        verify(configRepository, times(1)).existsByKey("MAX_SEATS");
        verify(configRepository, never()).save(any(Config.class));
    }


    @Test
    @DisplayName("Debe actualizar config correctamente")
    void shouldUpdateConfig() {
        // Given
        Config existingConfig = givenConfig("MAX_SEATS", "40");
        ConfigDtos.ConfigUpdateRequest request = givenUpdateRequest("50");

        when(configRepository.findById(1L)).thenReturn(Optional.of(existingConfig));
        when(configRepository.save(any(Config.class))).thenReturn(existingConfig);

        // When
        configService.updateConfig(1L, request);

        // Then
        verify(configRepository, times(1)).findById(1L);
        verify(configRepository, times(1)).save(existingConfig);
    }

    @Test
    @DisplayName("Debe lanzar excepción al actualizar config inexistente")
    void shouldThrowExceptionWhenUpdatingNonExistentConfig() {
        // Given
        ConfigDtos.ConfigUpdateRequest request = givenUpdateRequest("50");

        when(configRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> configService.updateConfig(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Configuración no encontrada");

        verify(configRepository, times(1)).findById(999L);
        verify(configRepository, never()).save(any(Config.class));
    }


    @Test
    @DisplayName("Debe eliminar config correctamente")
    void shouldDeleteConfig() {
        // Given
        Config config = givenConfig("OLD_KEY", "old_value");

        when(configRepository.findById(1L)).thenReturn(Optional.of(config));
        doNothing().when(configRepository).delete(config);

        // When
        configService.deleteConfig(1L);

        // Then
        verify(configRepository, times(1)).findById(1L);
        verify(configRepository, times(1)).delete(config);
    }

    @Test
    @DisplayName("Debe lanzar excepción al eliminar config inexistente")
    void shouldThrowExceptionWhenDeletingNonExistentConfig() {
        // Given
        when(configRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> configService.deleteConfig(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Configuración no encontrada");

        verify(configRepository, times(1)).findById(999L);
        verify(configRepository, never()).delete(any(Config.class));
    }


    @Test
    @DisplayName("Debe obtener todas las configuraciones")
    void shouldGetAllConfigs() {
        // Given
        List<Config> configs = List.of(
                givenConfig("KEY1", "value1"),
                givenConfig("KEY2", "value2")
        );

        when(configRepository.findAll()).thenReturn(configs);

        // When
        List<ConfigDtos.ConfigResponse> result = configService.getAllConfigs();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ConfigDtos.ConfigResponse::key)
                .containsExactlyInAnyOrder("KEY1", "KEY2");

        verify(configRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Debe obtener config por clave")
    void shouldGetConfigByKey() {
        // Given
        Config config = givenConfig("BAGGAGE_LIMIT", "20");

        when(configRepository.findByKey("BAGGAGE_LIMIT")).thenReturn(Optional.of(config));

        // When
        ConfigDtos.ConfigResponse response = configService.getConfigByKey("BAGGAGE_LIMIT");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.key()).isEqualTo("BAGGAGE_LIMIT");
        assertThat(response.value()).isEqualTo("20");

        verify(configRepository, times(1)).findByKey("BAGGAGE_LIMIT");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando clave no existe")
    void shouldThrowExceptionWhenKeyNotFound() {
        // Given
        when(configRepository.findByKey("NON_EXISTENT")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> configService.getConfigByKey("NON_EXISTENT"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Configuración con clave 'NON_EXISTENT' no encontrada");

        verify(configRepository, times(1)).findByKey("NON_EXISTENT");
    }


    @Test
    @DisplayName("Debe obtener valor como BigDecimal correctamente")
    void shouldGetValueAsBigDecimal() {
        // Given
        Config config = givenConfig("BAGGAGE_FEE", "3000.50");

        when(configRepository.findByKey("BAGGAGE_FEE")).thenReturn(Optional.of(config));

        // When
        BigDecimal result = configService.getValueAsBigDecimal("BAGGAGE_FEE");

        // Then
        assertThat(result).isEqualByComparingTo(new BigDecimal("3000.50"));

        verify(configRepository, times(1)).findByKey("BAGGAGE_FEE");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando valor no es BigDecimal válido")
    void shouldThrowExceptionWhenValueIsNotValidBigDecimal() {
        // Given
        Config config = givenConfig("INVALID_NUMBER", "not-a-number");

        when(configRepository.findByKey("INVALID_NUMBER")).thenReturn(Optional.of(config));

        // When & Then
        assertThatThrownBy(() -> configService.getValueAsBigDecimal("INVALID_NUMBER"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El valor de 'INVALID_NUMBER' no es un número válido");

        verify(configRepository, times(1)).findByKey("INVALID_NUMBER");
    }


    @Test
    @DisplayName("Debe obtener valor como Integer correctamente")
    void shouldGetValueAsInt() {
        // Given
        Config config = givenConfig("MAX_PASSENGERS", "40");

        when(configRepository.findByKey("MAX_PASSENGERS")).thenReturn(Optional.of(config));

        // When
        Integer result = configService.getValueAsInt("MAX_PASSENGERS");

        // Then
        assertThat(result).isEqualTo(40);

        verify(configRepository, times(1)).findByKey("MAX_PASSENGERS");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando valor no es Integer válido")
    void shouldThrowExceptionWhenValueIsNotValidInteger() {
        // Given
        Config config = givenConfig("INVALID_INT", "40.5");

        when(configRepository.findByKey("INVALID_INT")).thenReturn(Optional.of(config));

        // When & Then
        assertThatThrownBy(() -> configService.getValueAsInt("INVALID_INT"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El valor de 'INVALID_INT' no es un número entero válido");

        verify(configRepository, times(1)).findByKey("INVALID_INT");
    }


    @Test
    @DisplayName("Debe obtener valor como String correctamente")
    void shouldGetValueAsString() {
        // Given
        Config config = givenConfig("APP_NAME", "BusTransport");

        when(configRepository.findByKey("APP_NAME")).thenReturn(Optional.of(config));

        // When
        String result = configService.getValueAsString("APP_NAME");

        // Then
        assertThat(result).isEqualTo("BusTransport");

        verify(configRepository, times(1)).findByKey("APP_NAME");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando clave no existe para String")
    void shouldThrowExceptionWhenKeyNotFoundForString() {
        // Given
        when(configRepository.findByKey("MISSING_KEY")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> configService.getValueAsString("MISSING_KEY"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Configuración con clave 'MISSING_KEY' no encontrada");

        verify(configRepository, times(1)).findByKey("MISSING_KEY");
    }
}
