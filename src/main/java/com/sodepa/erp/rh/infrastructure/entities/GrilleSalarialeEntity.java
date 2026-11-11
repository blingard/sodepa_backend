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
/**
 * Entité de persistance représentant `GrilleSalarialeEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrilleSalarialeEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "GS";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }


    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Catégorie de rattachement administratif ou technique dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    private Integer categorie;

    /**
     * Champ echelon : echelon utilisé pour le pilotage et le suivi dans GrilleSalarialeEntity.
     */
    @Column(nullable = false, length = 2)
    private String echelon;

    /**
     * Champ groupe : groupe utilisé pour le pilotage et le suivi dans GrilleSalarialeEntity.
     */
    @Column(nullable = false)
    private Integer groupe;

    /**
     * Champ salaireBase : salaire base utilisé pour le pilotage et le suivi dans GrilleSalarialeEntity.
     */
    @Column(name = "salaire_base", nullable = false, precision = 15, scale = 2)
    private BigDecimal salaireBase;

    /**
     * Taux ou pourcentage (taux horaire) applicable au calcul.
     */
    @Column(name = "taux_horaire", precision = 10, scale = 2)
    private BigDecimal tauxHoraire;

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;
}
