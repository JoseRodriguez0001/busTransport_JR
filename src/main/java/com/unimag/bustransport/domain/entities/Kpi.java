package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "kpis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Kpi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private Double value;

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt = OffsetDateTime.now();
}
