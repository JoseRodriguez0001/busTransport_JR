package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus,Long> {
    Optional<Bus> findByPlate(String plate);
    List<Bus> findByStatus(String status);
    @Query("SELECT b " +
            "FROM Bus b " +
            "LEFT JOIN FETCH b.seats " +
            "WHERE b.id = :busId")
    Optional<Bus> findByIdWithSeats(@Param("busId") Long busId);
    @Modifying
    @Query("UPDATE Bus b " +
            "SET b.status = :status " +
            "WHERE b.id = :busId")
    void changeBusStatus(@Param("busId") Long busId, @Param("status") Bus.Status status);


}
