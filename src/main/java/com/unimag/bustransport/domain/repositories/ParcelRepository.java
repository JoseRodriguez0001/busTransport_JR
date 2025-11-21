package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParcelRepository extends JpaRepository<Parcel,Long> {
    List<Parcel> findByTripId(Long tripId);
    Optional<Parcel> findByCode(String code);
    List<Parcel> findBySenderPhone(String senderPhone);
    List<Parcel> findByReceiverPhone(String receiverPhone);
    boolean existsByCode(String code);
}
