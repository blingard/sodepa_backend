package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Règle de conversion contrôlée entre deux unités de mesure (REF-03 / F-REF-016 / CA-REF-016-01 & 02).
 * Exemple : 1 T = 1000 KG, 1 CARTON = 20 KG (pour un produit ou conditionnement donné).
 */
@Entity
@Table(name = "ref_conversions_unites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegleConversionUniteEntity {

    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_unite_source", nullable = false, length = 20)
    private String codeUniteSource;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_unite_cible", nullable = false, length = 20)
    private String codeUniteCible;

    /**
     * Facteur multiplication dans le contexte du traitement RegleConversionUniteEntity.
     */
    @Column(name = "facteur_multiplication", nullable = false, precision = 18, scale = 6)
    private BigDecimal facteurMultiplication;

    /**
     * Code produit optionnel lorsque la conversion dépend d'un conditionnement métier (ex: 1 CARTON = 20 KG).
     */
    @Column(name = "code_produit_specifique", length = 50)
    private String codeProduitSpecifique;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
