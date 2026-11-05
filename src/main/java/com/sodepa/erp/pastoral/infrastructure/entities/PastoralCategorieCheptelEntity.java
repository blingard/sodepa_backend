package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pastoral_categories_cheptel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralCategorieCheptelEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(name = "age_min_mois")
    private Integer ageMinMois;

    @Column(name = "age_max_mois")
    private Integer ageMaxMois;

    @Column(name = "compte_actif_syscohada", nullable = false, length = 30)
    private String compteActifSyscohada;

    @Column(name = "compte_variation_syscohada", nullable = false, length = 30)
    private String compteVariationSyscohada;

    @Column(name = "valeur_forfaitaire_bareme", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurForfaitaireBareme = BigDecimal.ZERO;
}
