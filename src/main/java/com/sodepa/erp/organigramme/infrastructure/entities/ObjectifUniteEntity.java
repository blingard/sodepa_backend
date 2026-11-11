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
/**
 * Entité de persistance représentant `ObjectifUniteEntity` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectifUniteEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "OU";

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
     * Unité de mesure de référence (kg, tête, litre, sac, unité) dans le cadre des opérations SODEPA.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unite_id", nullable = false)
    private UniteOrganisationnelleEntity unite;

    /**
     * Intitulé officiel ou désignation dans le cadre des opérations SODEPA.
     */
    @Column(nullable = false, length = 150)
    private String intitule;

    /**
     * Description détaillée et informations contextuelles dans le cadre des opérations SODEPA.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Champ indicateur : indicateur utilisé pour le pilotage et le suivi dans ObjectifUniteEntity.
     */
    @Column(length = 100)
    private String indicateur;

    /**
     * Unité de mesure réglementaire dans le cadre des opérations SODEPA.
     */
    @Column(name = "unite_mesure", length = 30)
    @Builder.Default
    private String uniteMesure = "UNITES";

    /**
     * Champ valeurCible : valeur cible utilisé pour le pilotage et le suivi dans ObjectifUniteEntity.
     */
    @Column(name = "valeur_cible", nullable = false, precision = 15, scale = 2)
    private BigDecimal valeurCible;

    /**
     * Champ valeurRealisee : valeur realisee utilisé pour le pilotage et le suivi dans ObjectifUniteEntity.
     */
    @Column(name = "valeur_realisee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal valeurRealisee = BigDecimal.ZERO;

    /**
     * Taux ou pourcentage (taux atteinte) applicable au calcul.
     */
    @Column(name = "taux_atteinte", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tauxAtteinte = BigDecimal.ZERO;

    /**
     * Champ anneeExercice : annee exercice utilisé pour le pilotage et le suivi dans ObjectifUniteEntity.
     */
    @Column(name = "annee_exercice", nullable = false)
    private Integer anneeExercice;

    /**
     * Date d'échéance contractuelle ou réglementaire dans le cadre des opérations SODEPA.
     */
    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    /**
     * Statut dans le cycle de vie de gestion au sein de la SODEPA dans le cadre des opérations SODEPA.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatutObjectif statut = StatutObjectif.NON_DEMARRE;

    /**
     * Nom ou désignation textuelle de responsable pour l'affichage et la traçabilité.
     */
    @Column(name = "responsable_nom", length = 100)
    private String responsableNom;

    /**
     * Champ createdAt : created at utilisé pour le pilotage et le suivi dans ObjectifUniteEntity.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Champ updatedAt : updated at utilisé pour le pilotage et le suivi dans ObjectifUniteEntity.
     */
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
