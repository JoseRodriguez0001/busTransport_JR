package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false,unique = true)
    private String email;
    @Column(unique = true,length = 30)
    private String phone;
    @Column(nullable = false,name = "password_hash")
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Passenger> passengers = new ArrayList<>();

    public void addPassenger(Passenger passenger) {
        this.passengers.add(passenger);
        passenger.setUser(this);
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        BLOCKED
    }

}
