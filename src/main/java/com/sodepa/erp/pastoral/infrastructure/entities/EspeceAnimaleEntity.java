package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Référentiel des espèces animales élevées ou traitées par la SODEPA (REF-03 / F-REF-013 / CA-REF-013-01 & 02).
 */
@Entity
@Table(name = "ref_zoo_especes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EspeceAnimaleEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "EA";

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
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 120)
    private String libelle;

    /**
     * Nom scientifique dans le contexte du traitement EspeceAnimaleEntity.
     */
    @Column(name = "nom_scientifique", length = 120)
    private String nomScientifique;

    /**
     * Duree gestation jours dans le contexte du traitement EspeceAnimaleEntity.
     */
    @Column(name = "duree_gestation_jours")
    private Integer dureeGestationJours;

    /**
     * Age sevrage mois dans le contexte du traitement EspeceAnimaleEntity.
     */
    @Column(name = "age_sevrage_mois")
    private Integer ageSevrageMois;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
