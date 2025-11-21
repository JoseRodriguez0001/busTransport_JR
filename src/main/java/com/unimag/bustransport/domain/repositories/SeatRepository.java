package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat,Long> {
    List<Seat> findByBusIdOrderByNumberAsc(Long busId);
    Optional<Seat> findByBusIdAndNumber(Long busId, String number);
    List<Seat> findByBusIdAndType(Long busId, Seat.Type type);
    long countByBusId(Long busId);

}
