package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seat_bus_number",
                columnNames = {"bus_id", "number"}
        ))
@Getter@Setter
@NoArgsConstructor@AllArgsConstructor@Builder
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String number;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @ManyToOne
    @JoinColumn(name = "bus_id",foreignKey = @ForeignKey(name = "fk_seat_bus"))
    private Bus bus;



    public enum Type{
        STANDARD,
        PREFERENTIAL
    }
}
