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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DerogationPermissionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private UtilisateurEntity utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private Permissions permission;

    @Column(name = "date_debut", nullable = false)
    private Instant dateDebut;

    @Column(name = "date_fin", nullable = false)
    private Instant dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutDerogation statut = StatutDerogation.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "reference_decision", length = 150)
    private String referenceDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accorde_par_id")
    private UtilisateurEntity accordePar;

    @Column(name = "date_creation", nullable = false, updatable = false)
    @Builder.Default
    private Instant dateCreation = Instant.now();
}
