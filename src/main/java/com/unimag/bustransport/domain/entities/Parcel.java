package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "parcels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parcel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.CREATED;

    @Column(name = "proof_photo_url")
    private String proofPhotoUrl;

    @Column(name = "delivery_otp")
    private String deliveryOtp;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(name = "sender_phone")
    private String senderPhone;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_parcel_trip"))
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_stop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_parcel_fromstop"))
    private Stop fromStop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_stop_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_parcel_tostop"))
    private Stop toStop;

    public enum Status {
        CREATED, IN_TRANSIT, DELIVERED, FAILED
    }
}
