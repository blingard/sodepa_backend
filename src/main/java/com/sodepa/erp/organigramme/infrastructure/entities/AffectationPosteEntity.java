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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AffectationPosteEntity implements Persistable<UUID> {

    @Id
    private UUID id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private UtilisateurEntity utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poste_id", nullable = false)
    private PosteEntity poste;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_affectation", nullable = false, length = 30)
    private TypeAffectation typeAffectation;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "reference_decision", length = 150)
    private String referenceDecision;

    @Column(name = "motif_fin", length = 100)
    private String motifFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutAffectation statut = StatutAffectation.ACTIF;
}
