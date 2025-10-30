package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.stereotype.Service;

@Entity
@Table(name = "seats")
@Getter@Setter
@NoArgsConstructor@AllArgsConstructor@Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bus_id",foreignKey = @ForeignKey(name = "fk_seat_bus"))
    private Bus bus;

    @Column(nullable = false)
    private String number;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    public enum Type{
        STANDARD,
        PREFERENTIAL
    }
}
