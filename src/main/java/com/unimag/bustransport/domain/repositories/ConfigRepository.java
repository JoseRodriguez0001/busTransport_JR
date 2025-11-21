package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Config;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfigRepository extends JpaRepository<Config, Long> {
    Optional<Config> findByKey(String key);
    boolean existsByKey(String key);
}
