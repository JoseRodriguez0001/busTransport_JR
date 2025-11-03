package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "baggage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Baggage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Column
    private BigDecimal fee;

    @Column(name = "tag_code", unique = true)
    private String tagCode;

    @OneToOne
    @JoinColumn(name = "ticket_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_baggage_ticket"))
    private Ticket ticket;
}
