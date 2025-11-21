package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false)
    private LocalDate date;
    @Column(nullable = false, name = "departure_at")
    private OffsetDateTime departureAt;
    @Column(nullable = false, name = "arrival_at")
    private OffsetDateTime arrivalAt;
    private Double overbookingPercent;
    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    @ManyToOne
    @JoinColumn(name = "route_id",foreignKey = @ForeignKey(name = "fk_trip_route"))
    private Route route;

    @ManyToOne
    @JoinColumn(name = "bus_id",foreignKey = @ForeignKey(name = "fk_trip_bus"))
    private Bus bus;

    @OneToMany(mappedBy = "trip",  fetch = FetchType.LAZY)
    private List<SeatHold> seatHolds= new ArrayList<>();

    public void addSeatHold(SeatHold seatHold) {
        this.seatHolds.add(seatHold);
        seatHold.setTrip(this);
    }

    @OneToMany(mappedBy = "trip",fetch = FetchType.LAZY)
    private List<Ticket> tickets= new ArrayList<>();

    public void addTicket(Ticket ticket) {
        this.tickets.add(ticket);
        ticket.setTrip(this);
    }

    @OneToMany(mappedBy = "trip", fetch = FetchType.LAZY)
    private List<Parcel> parcels= new ArrayList<>();

    public void addParcel(Parcel parcel) {
        this.parcels.add(parcel);
        parcel.setTrip(this);
    }

    public enum Status {
        SCHEDULED,
        BOARDING,
        DEPARTED,
        ARRIVED,
        CANCELLED
    }
}
