package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "route_id",foreignKey = @ForeignKey(name = "fk_trip_route"))
    private Route route;

    @ManyToOne
    @JoinColumn(name = "bus_id",foreignKey = @ForeignKey(name = "fk_trip_bus"))
    private Bus bus;

    @Column(nullable = false)
    private LocalDate date;
    @Column(nullable = false, name = "departure_at")
    private OffsetDateTime departureAt;
    @Column(nullable = false, name = "arrival_at")
    private OffsetDateTime arrivalAt;
    private Double overbookingPercent;
    private Status status = Status.SCHEDULED;

    public enum Status {
        SCHEDULED,
        BOARDING,
        DEPARTED,
        ARRIVED,
        CANCELLED
    }
}
