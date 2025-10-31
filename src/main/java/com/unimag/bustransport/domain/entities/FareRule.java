package com.unimag.bustransport.domain.entities;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


@Entity
@Table(name = "fare_rules")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FareRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal basePrice;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Double> discounts = new HashMap<>();

    @Enumerated(EnumType.STRING)
    private DinamyPricing dinamyPricing;

    @ManyToOne
    @JoinColumn(name = "route_id",foreignKey = @ForeignKey(name = "fk_farerule_route"))
    private Route route;

    @ManyToOne
    @JoinColumn(name = "from_stop_id",foreignKey = @ForeignKey(name = "fk_farerule_fromstop"))
    private Stop fromStop;
    @ManyToOne
    @JoinColumn(name = "to_stop_id",foreignKey = @ForeignKey(name = "fk_farerule_tostop"))
    private Stop toStop;

    public enum DinamyPricing{
        ON,
        OFF
    }
}
