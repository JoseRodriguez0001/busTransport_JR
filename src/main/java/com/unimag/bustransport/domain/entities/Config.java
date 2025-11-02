package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Config {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;
}
