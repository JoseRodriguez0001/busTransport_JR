package com.unimag.bustransport.domain.entities;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "buses")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Bus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String plate;
    @Column(nullable = false)
    private Integer capacity;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private List<String> amenities = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Status  status;
    public enum Status {
        ACTIVE,
        IN_REPAIR,
        RETIRED
    }

    @OneToMany(mappedBy = "bus", fetch = FetchType.LAZY)
    private List<Trip> trips;
    public void addTrip(Trip trip) {
        this.trips.add(trip);
        trip.setBus(this);
    }

    @OneToMany(mappedBy = "bus",fetch = FetchType.LAZY,cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seat> seats = new ArrayList<>();
    public void addSeat(Seat seat) {
        this.seats.add(seat);
        seat.setBus(this);
    }
}
