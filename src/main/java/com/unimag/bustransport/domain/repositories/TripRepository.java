package com.unimag.bustransport.domain.repositories;

import com.unimag.bustransport.domain.entities.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip,Long> {
    List<Trip> findByRouteIdAndDateAndStatus(Long routeId, LocalDate date, Trip.Status status);

    @Query("SELECT t " +
            "FROM Trip t " +
            "WHERE t.route.origin = :origin " +
            "      AND t.route.destination = :destination " +
            "      AND t.date = :date " +
            "      AND t.status = com.unimag.bustransport.domain.entities.Trip.Status.SCHEDULED")
    List<Trip> findTripsByOriginAndDestination(@Param("origin") String origin,
                                               @Param("destination") String destination,
                                               @Param("date") LocalDate date);

    List<Trip> findByStatus(Trip.Status status);
    List<Trip> findByBusIdAndStatus(Long busId, Trip.Status status);

    // viajes que estan proximos a salir.
    @Query( "SELECT t " +
            "FROM Trip t " +
            "WHERE t.date = :date " +
            "      AND t.status = com.unimag.bustransport.domain.entities.Trip.Status.SCHEDULED " +
            "      AND t.departureAt <= :threshold")
            List<Trip> findTripsNearDeparture(@Param("date") LocalDate date, @Param("threshold") OffsetDateTime threshold);

    //encontrar viaje que con bus y sus asientos para ver su disponibilidad
    @Query( "SELECT t " +
            "FROM Trip t " +
            "LEFT JOIN FETCH t.bus b " +
            "LEFT JOIN FETCH b.seats " +
            "WHERE t.id = :tripId")
            Optional<Trip> findByIdWithBusAndSeats(@Param("tripId") Long tripId);

    @Query("SELECT COUNT(ti)" +
            "FROM Ticket ti " +
            "WHERE ti.trip.id = :tripId       AND ti.status IN (\n" +
            "            com.unimag.bustransport.domain.entities.Ticket.Status.SOLD,\n" +
            "            com.unimag.bustransport.domain.entities.Ticket.Status.BOARDED\n" +
            "      )")
    Long countSoldTickets(Long tripId);

}
