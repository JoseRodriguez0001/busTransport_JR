package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;


@Entity
@Table(name = "passengers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Passenger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name",nullable = false)
    private String fullName;
    @Column(name = "document_type")
    private String documentType;
    @Column(name = "document_number")
    private String documentNumber;
    @Column(name = "birth_date")
    private LocalDate birthDate;
    @Column(name ="phone_number")
    private String phoneNumber;
    @Column(name = "create_at")
    private OffsetDateTime createAt;
    @ManyToOne
    @JoinColumn(name = "user_id",foreignKey = @ForeignKey(name = "fk_passenger_user"))
    private User user;
}
