package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Catalogue des produits, articleées et sous-produits animaux SODEPA
 * (REF-03 / F-REF-014 / CA-REF-014-01 & 02 / RG-REF-002).
 */
@Entity
@Table(name = "ref_catalogue_produits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogueProduitAnimalEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "CPA";

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
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Désignation normalisée de l'articleée ou de la prestation.
     */
    @Column(nullable = false, length = 200)
    private String designation;

    /**
     * Catégorie : ANIMAL_VIF, CARCASSE, DECOUPE_VIANDE, ABAT_CINQUIEME_QUARTIER,
     * SOUS_PRODUIT_CUIR_PEAU, PRODUIT_LAITIER, INTRANT_ALIMENT_VET.
     */
    @Column(name = "categorie_produit", nullable = false, length = 50)
    private String categorieProduit;

    /**
     * Code mnémonique du espèce animale du référentiel zootechnique (Bovins, Porcins, Caprins, etc.).
     */
    @Column(name = "code_espece", length = 30)
    private String codeEspece;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "code_unite_mesure", nullable = false, length = 20)
    private String codeUniteMesure;

    /**
     * Mode de conservation : FRAIS_REFRIGERE_0_4C, CONGELE_MOINS_18C, AMBIANT, SUR_PIED.
     */
    @Column(name = "mode_conservation", nullable = false, length = 40)
    private String modeConservation;

    /**
     * Duree conservation jours dans le contexte du traitement CatalogueProduitAnimalEntity.
     */
    @Column(name = "duree_conservation_jours")
    private Integer dureeConservationJours;

    /**
     * Prix unitaire ou global en Francs CFA.
     */
    @Column(name = "prix_reference_ht_fcfa", precision = 15, scale = 2)
    private BigDecimal prixReferenceHtFcfa;

    /**
     * Compte comptable conforme au plan comptable OHADA révisé.
     */
    @Column(name = "compte_produit_syscohada", length = 20)
    @Builder.Default
    private String compteProduitSyscohada = "7011";

    /**
     * Statut commercial : COMMERCIALISABLE, USAGE_INTERNE, SUSPENDU, ARCHIVE.
     */
    @Column(name = "statut_commercial", nullable = false, length = 30)
    @Builder.Default
    private String statutCommercial = "COMMERCIALISABLE";

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
