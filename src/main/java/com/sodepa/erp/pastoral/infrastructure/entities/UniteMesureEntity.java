package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Référentiel des Unités de Mesure SODEPA (REF-03 / F-REF-016 / CA-REF-016-01 & 02 / §10).
 * Chaque unité est rattachée à une dimension physique permettant de bloquer les conversions incohérentes.
 */
@Entity
@Table(name = "ref_unites_mesure")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniteMesureEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "UM";

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
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /**
     * Libellé cleéair et descriptif.
     */
    @Column(nullable = false, length = 100)
    private String libelle;

    /**
     * Symbole dans le contexte du traitement UniteMesureEntity.
     */
    @Column(nullable = false, length = 20)
    private String symbole;

    /**
     * Dimension physique : MASSE_POIDS, VOLUME, EFFECTIF_UNITE, CONDITIONNEMENT, SUPERFICIE, TEMPS, ENERGIE.
     */
    @Column(name = "dimension_physique", nullable = false, length = 40)
    private String dimensionPhysique;

    /**
     * Decimales autorisees dans le contexte du traitement UniteMesureEntity.
     */
    @Column(name = "decimales_autorisees", nullable = false)
    @Builder.Default
    private Integer decimalesAutorisees = 2;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
