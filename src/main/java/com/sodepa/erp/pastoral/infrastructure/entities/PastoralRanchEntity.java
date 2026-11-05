package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pastoral_ranches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralRanchEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 100)
    private String region;

    @Column(name = "superficie_hectares", precision = 12, scale = 2)
    private BigDecimal superficieHectares;

    @Column(name = "capacite_tetes")
    private Integer capaciteTetes;

    @Column(name = "responsable_nom", length = 150)
    private String responsableNom;

    @Column(name = "regie_plafond_fcfa", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal regiePlafondFcfa = BigDecimal.valueOf(10_000_000);

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
