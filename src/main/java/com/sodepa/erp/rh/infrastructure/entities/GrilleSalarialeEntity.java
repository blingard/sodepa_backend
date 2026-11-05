package com.sodepa.erp.rh.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Grille indiciaire de rémunération SODEPA par catégorie (1 à 12) et échelon (A à F)
 * conforme à la Convention collective et au Statut du Personnel SODEPA.
 */
@Entity
@Table(name = "rh_grille_salariale", uniqueConstraints = {
    @UniqueConstraint(name = "uq_grille_cat_echelon", columnNames = {"categorie", "echelon"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrilleSalarialeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Integer categorie;

    @Column(nullable = false, length = 2)
    private String echelon;

    @Column(nullable = false)
    private Integer groupe;

    @Column(name = "salaire_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireBase;

    @Column(name = "taux_horaire", precision = 10, scale = 2)
    private BigDecimal tauxHoraire;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
