package com.sodepa.erp.pastoral.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité de persistance représentant `PastoralRanchEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Entity
@Table(name = "pastoral_ranches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PastoralRanchEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "RCH";

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
    private UUID id;

    /**
     * Code de référence unique normalisé dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    /**
     * Nom officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String nom;

    /**
     * Région administrative du Cameroun (ex: Adamaoua, Nord, Extrême-Nord, Centre) dans le cadre des opérations SODEPA.
     */
    @Column(length = 100)
    private String region;

    /**
     * Champ superficieHectares : superficie hectares utilisé pour le pilotage et le suivi dans PastoralRanchEntity.
     */
    @Column(name = "superficie_hectares", precision = 12, scale = 2)
    private BigDecimal superficieHectares;

    /**
     * Champ capaciteTetes : capacite tetes utilisé pour le pilotage et le suivi dans PastoralRanchEntity.
     */
    @Column(name = "capacite_tetes")
    private Integer capaciteTetes;

    /**
     * Nom ou désignation textuelle de responsable pour l'affichage et la traçabilité.
     */
    @Column(name = "responsable_nom", length = 150)
    private String responsableNom;

    /**
     * Champ regiePlafondFcfa : regie plafond fcfa utilisé pour le pilotage et le suivi dans PastoralRanchEntity.
     */
    @Column(name = "regie_plafond_fcfa", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal regiePlafondFcfa = BigDecimal.valueOf(10_000_000);

    /**
     * Indicateur d'activation opérationnelle (true si actif, false si archivé/désactivé) dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
