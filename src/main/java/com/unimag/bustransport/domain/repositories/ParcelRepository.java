package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Parcel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ParcelRepository extends JpaRepository<Parcel,Long> {
    List<Parcel> findByTripId(Long tripId);
    Optional<Parcel> findByCode(String code);
    List<Parcel> findByStatus(Parcel.Status status);
    List<Parcel> findBySenderPhone(String senderPhone);

    @Query(value = "SELECT * FROM parcels WHERE status = 'IN_TRANSIT' ORDER BY id DESC", nativeQuery = true)
    List<Parcel> findInTransitNative();

    List<Parcel> findByReceiverPhone(String receiverPhone);
    List<Parcel> findByTripIdAndStatus(Long tripId, Parcel.Status status);

    boolean existsByCode(String code);
}
