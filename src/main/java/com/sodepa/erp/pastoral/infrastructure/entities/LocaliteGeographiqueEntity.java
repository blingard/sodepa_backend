package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Découpage administratif et géographique du Cameroun (REF-02 / F-REF-012 / CA-REF-012-01) :
 * Région, Département, Arrondissement, Localité et Zone agro-écologique.
 */
@Entity
@Table(name = "ref_geo_localites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocaliteGeographiqueEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "LG";

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
     * Nom dans le contexte du traitement LocaliteGeographiqueEntity.
     */
    @Column(nullable = false, length = 150)
    private String nom;

    /**
     * Niveau administratif : REGION, DEPARTEMENT, ARRONDISSEMENT, LOCALITE.
     */
    @Column(name = "niveau_administratif", nullable = false, length = 30)
    private String niveauAdministratif;

    /**
     * Code mnémonique unique normalisé.
     */
    @Column(name = "parent_code", length = 40)
    private String parentCode;

    /**
     * Zone agro-écologique : SOUDANO_SAHELIENNE, HAUTES_SAVANES_GUINEENNES, HAUTS_PLATEAUX_OUEST, FORET_BIMODALE, FORET_MONOMODALE.
     */
    @Column(name = "zone_agro_ecologique", length = 60)
    private String zoneAgroEcologique;

    /**
     * Indicateur booléen d'activation de l'élément (évite la suppression physique).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
