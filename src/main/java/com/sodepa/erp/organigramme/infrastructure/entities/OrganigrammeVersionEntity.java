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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganigrammeVersionEntity {

    @Id
    private UUID id;

    @Column(name = "numero_version", nullable = false, unique = true, length = 50)
    private String numeroVersion;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_effet", nullable = false)
    private LocalDate dateEffet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutVersionOrganigramme statut = StatutVersionOrganigramme.BROUILLON;

    @Column(name = "approuve_par")
    private UUID approuvePar;

    @Column(name = "date_approbation")
    private Instant dateApprobation;
}
