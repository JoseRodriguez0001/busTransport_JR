package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger,Long> {
    List<Passenger> findByUserId(Long userId);
    Optional<Passenger> findByDocumentNumber(String documentNumber);
    boolean existsByDocumentNumber(String documentNumber);
}
