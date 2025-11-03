package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Bus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BusRepository extends JpaRepository<Bus,Long> {
    Optional<Bus> findByPlate(String plate);
    List<Bus> finByStatus(String status);
    @Query("SELECT b " +
            "FROM Bus b " +
            "LEFT JOIN FETCH b.seats " +
            "WHERE b.id = :busId")
            Optional<Bus> findByIdWithSeats(@Param("busId") Long busId);
    void changeBusStatus(Long busId, Bus.Status status);


}
