package com.sodepa.erp.organigramme.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Objectif opérationnel assigné à une unité organisationnelle.
 * Permet d'évaluer la performance et l'atteinte des missions pour un exercice budgétaire/annuel.
 */
@Entity
@Table(name = "objectifs_unites", indexes = {
    @Index(name = "idx_objectif_unite", columnList = "unite_id"),
    @Index(name = "idx_objectif_exercice", columnList = "annee_exercice")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectifUniteEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unite_id", nullable = false)
    private UniteOrganisationnelleEntity unite;

    @Column(nullable = false, length = 150)
    private String intitule;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String indicateur;

    @Column(name = "unite_mesure", length = 30)
    @Builder.Default
    private String uniteMesure = "UNITES";

    @Column(name = "valeur_cible", nullable = false, precision = 15, scale = 2)
    private BigDecimal valeurCible;

    @Column(name = "valeur_realisee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurRealisee = BigDecimal.ZERO;

    @Column(name = "taux_atteinte", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxAtteinte = BigDecimal.ZERO;

    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutObjectif statut = StatutObjectif.NON_DEMARRE;

    @Column(name = "responsable_nom", length = 100)
    private String responsableNom;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void recalculerTaux() {
        if (this.valeurCible != null && this.valeurCible.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal realise = this.valeurRealisee != null ? this.valeurRealisee : BigDecimal.ZERO;
            this.tauxAtteinte = realise.multiply(BigDecimal.valueOf(100))
                    .divide(this.valeurCible, 2, RoundingMode.HALF_UP);
            
            if (this.tauxAtteinte.compareTo(BigDecimal.valueOf(100)) >= 0) {
                this.statut = StatutObjectif.ATTEINT;
            } else if (this.tauxAtteinte.compareTo(BigDecimal.ZERO) > 0) {
                this.statut = StatutObjectif.EN_COURS;
            }
        }
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.valeurRealisee == null) {
            this.valeurRealisee = BigDecimal.ZERO;
        }
        recalculerTaux();
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        recalculerTaux();
        this.updatedAt = LocalDateTime.now();
    }
}
