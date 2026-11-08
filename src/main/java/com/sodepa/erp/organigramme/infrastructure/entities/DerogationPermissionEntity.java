package com.sodepa.erp.organigramme.infrastructure.entities;

import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import com.sodepa.erp.utils.Permissions;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entité matérialisant une dérogation nominative d'habilitation accordée par la Direction Générale.
 * Permet d'octroyer temporairement des permissions exceptionnelles à un collaborateur
 * sans modifier son poste budgété dans l'organigramme.
 */
@Entity
@Table(name = "derogations_permissions", indexes = {
    @Index(name = "idx_derogation_user", columnList = "utilisateur_id"),
    @Index(name = "idx_derogation_statut", columnList = "statut"),
    @Index(name = "idx_derogation_dates", columnList = "date_debut, date_fin")
})
/**
 * Entité de persistance représentant `DerogationPermissionEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DerogationPermissionEntity {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Champ utilisateur : utilisateur utilisé pour le pilotage et le suivi dans DerogationPermissionEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private UtilisateurEntity utilisateur;

    /**
     * Champ permission : permission utilisé pour le pilotage et le suivi dans DerogationPermissionEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Permissions permission;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut", nullable = false)
    private Instant dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_fin", nullable = false)
    private Instant dateFin;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutDerogation statut = StatutDerogation.ACTIVE;

    /**
     * Motif justificatif de l'opération ou de la décision dans le cadre des opérations SODEPA.
     */
    @Column(columnDefinition = "TEXT")
    private String motif;

    /**
     * Champ referenceDecision : reference decision utilisé pour le pilotage et le suivi dans DerogationPermissionEntity.
     */
    @Column(name = "reference_decision", length = 150)
    private String referenceDecision;

    /**
     * Champ accordePar : accorde par utilisé pour le pilotage et le suivi dans DerogationPermissionEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accorde_par_id")
    private UtilisateurEntity accordePar;

    /**
     * Date et heure de création de l'enregistrement dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
