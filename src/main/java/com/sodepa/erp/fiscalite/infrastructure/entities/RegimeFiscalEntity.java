package com.sodepa.erp.fiscalite.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Référentiel des régimes d'imposition DGI Cameroun (F-REF-020).
 * Détermine automatiquement le taux d'Acompte sur Impôt sur le Revenu (AIR) et l'assujettissement TVA des tiers.
 */
@Entity
@Table(name = "ref_fiscalite_regimes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegimeFiscalEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "RF";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Taux ou coefficient en pourcentage.
     */
    @Column(name = "taux_air_applicable", nullable = false, precision = 6, scale = 2)
    private BigDecimal tauxAirApplicable;

    /**
     * Taxe sur la Valeur Ajoutée (TVA DGI Cameroun).
     */
    @Column(name = "assujetti_tva", nullable = false)
    @Builder.Default
    private boolean assujettiTva = true;

    /**
     * Description détaillée des caractéristiques.
     */
    @Column(length = 300)
    private String description;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
