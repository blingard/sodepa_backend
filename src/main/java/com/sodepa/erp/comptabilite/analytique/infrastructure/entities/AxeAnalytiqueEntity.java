package com.sodepa.erp.comptabilite.analytique.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

/**
 * Entité représentant un axe analytique au sein du plan analytique (ex: Projet, Centre de coût, Département).
 * Permet de segmenter l'analyse de performance et d'imputer les charges/produits selon des dimensions de gestion.
 */
@Entity
@Table(name = "axes_analytiques")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AxeAnalytiqueEntity {

    /**
     * Identifiant unique de l'axe analytique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Code abrégé de l'axe (ex: 'PROJETS', 'DEPARTEMENTS', 'CENTRES_DE_COUTS').
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Intitulé ou libellé décrivant l'axe analytique.
     */
    @Column(nullable = false)
    private String intitule;

    /**
     * Statut d'activation de l'axe. Un axe inactif ne peut plus recevoir d'imputations.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
