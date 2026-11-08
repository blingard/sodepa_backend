package com.sodepa.erp.organigramme.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité de versioning temporel de l'organigramme SODEPA (Effective Dating).
 */
@Entity
@Table(name = "organigramme_versions", indexes = {
    @Index(name = "idx_org_version_num", columnList = "numero_version", unique = true),
    @Index(name = "idx_org_version_statut", columnList = "statut")
})
/**
 * Entité de persistance représentant `OrganigrammeVersionEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganigrammeVersionEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ numeroVersion : numero version utilisé pour le pilotage et le suivi dans OrganigrammeVersionEntity.
     */
    @Column(name = "numero_version", nullable = false, unique = true, length = 50)
    private String numeroVersion;

    /**
     * Libellé descriptif officiel dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String libelle;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Champ dateEffet : date effet utilisé pour le pilotage et le suivi dans OrganigrammeVersionEntity.
     */
    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutVersionOrganigramme statut = StatutVersionOrganigramme.BROUILLON;

    /**
     * Champ approuvePar : approuve par utilisé pour le pilotage et le suivi dans OrganigrammeVersionEntity.
     */
    @Column(name = "approuve_par")
    private UUID approuvePar;

    /**
     * Champ dateApprobation : date approbation utilisé pour le pilotage et le suivi dans OrganigrammeVersionEntity.
     */
    @Column(name = "date_approbation")
    private Instant dateApprobation;
}
