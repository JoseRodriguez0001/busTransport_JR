package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "routes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String code;
    private String name;
    @Column(nullable = false)
    private String origin;
    @Column(nullable = false)
    private String destination;
    @Column(nullable = false,name = "distance_km")
    private Double distanceKm;
    @Column(nullable = false,name = "duration_min")
    private Integer durationMin;

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    private List<Stop> stops = new ArrayList<>();

    public void addStop(Stop stop) {
        this.stops.add(stop);
        stop.setRoute(this);
    }

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    private List<Trip>  trips = new ArrayList<>();
    public void addTrip(Trip trip) {
        this.trips.add(trip);
        trip.setRoute(this);
    }

    @OneToMany(mappedBy = "route", fetch = FetchType.LAZY)
    private List<FareRule> fareRules = new ArrayList<>();
    public void addFareRule(FareRule fareRule) {
        this.fareRules.add(fareRule);
        fareRule.setRoute(this);
    }

}
