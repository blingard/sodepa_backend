package com.sodepa.erp.organigramme.infrastructure.entities;

import com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entité matérialisant l'affectation d'un collaborateur (UtilisateurEntity) à un PosteEntity.
 */
@Entity
@Table(name = "affectations_postes", indexes = {
    @Index(name = "idx_aff_user", columnList = "utilisateur_id"),
    @Index(name = "idx_aff_poste", columnList = "poste_id"),
    @Index(name = "idx_aff_statut", columnList = "statut")
})
/**
 * Entité de persistance représentant `AffectationPosteEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationPosteEntity implements Persistable<UUID> {

    /**
     * Identifiant unique technique dans le cadre des opérations SODEPA.
     */
    @Id
    private UUID id;

    /**
     * Indicateur booléen spécifiant si isNew est effectif ou applicable.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    /**
     * Champ utilisateur : utilisateur utilisé pour le pilotage et le suivi dans AffectationPosteEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private UtilisateurEntity utilisateur;

    /**
     * Champ poste : poste utilisé pour le pilotage et le suivi dans AffectationPosteEntity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id", nullable = false)
    private PosteEntity poste;

    /**
     * Champ typeAffectation : type affectation utilisé pour le pilotage et le suivi dans AffectationPosteEntity.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_affectation", nullable = false, length = 30)
    private TypeAffectation typeAffectation;

    /**
     * Date de début d'effet ou de validité dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    /**
     * Date de fin d'effet ou échéance dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_fin")
    private LocalDate dateFin;

    /**
     * Champ referenceDecision : reference decision utilisé pour le pilotage et le suivi dans AffectationPosteEntity.
     */
    @Column(name = "reference_decision", length = 150)
    private String referenceDecision;

    /**
     * Champ motifFin : motif fin utilisé pour le pilotage et le suivi dans AffectationPosteEntity.
     */
    @Column(name = "motif_fin", length = 100)
    private String motifFin;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutAffectation statut = StatutAffectation.ACTIF;
}
