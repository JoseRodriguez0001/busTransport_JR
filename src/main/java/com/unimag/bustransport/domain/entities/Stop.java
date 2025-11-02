package com.unimag.bustransport.domain.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stops")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Stop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private Integer order;
    private Double lat;
    private Double lng;

    @ManyToOne
    @JoinColumn(name = "route_id",foreignKey = @ForeignKey(name = "fk_stop_route"))
    private Route route;

    // Reglas donde esta parada es el origen (from_stop)
    @OneToMany(mappedBy = "fromStop", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<FareRule> fareRulesFrom = new ArrayList<>();
    public void addFareRuleFrom(FareRule fareRule) {
        this.fareRulesFrom.add(fareRule);
        fareRule.setFromStop(this);
    }

    // Reglas donde esta parada es el destino (to_stop)
    @OneToMany(mappedBy = "toStop", fetch = FetchType.LAZY, orphanRemoval = true)
    private List<FareRule> fareRulesTo = new ArrayList<>();
    public void addFareRuleTo(FareRule fareRule) {
        this.fareRulesTo.add(fareRule);
        fareRule.setToStop(this);
    }

}

