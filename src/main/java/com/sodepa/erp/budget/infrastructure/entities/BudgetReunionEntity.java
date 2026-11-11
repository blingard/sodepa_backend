package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité matérialisant une séance de travail / réunion de concertation budgétaire
 * avec traçabilité de l'émargement et du procès-verbal (CA-BUD-005-01 et CA-BUD-005-02).
 */
@Entity
@Table(name = "budget_reunion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetReunionEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "REU-BUD";

    public String getPrefixeCode() {
        return this.codePrefix;
    }

    public void setPrefixeCode(String prefixeCode) {
        this.codePrefix = prefixeCode;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Groupe de travail budgétaire associé.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupe_travail_id", nullable = false)
    private BudgetGroupeTravailEntity groupeTravail;

    /**
     * Date et heure de tenue de la séance.
     */
    @Column(name = "date_reunion", nullable = false)
    private LocalDateTime dateReunion;

    /**
     * Ordre du jour de la concertation.
     */
    @Column(name = "ordre_du_jour", nullable = false, length = 255)
    private String ordreDuJour;

    /**
     * Lieu ou salle de réunion (ex: Salle des Actes SODEPA Yaoundé / Visioconférence).
     */
    @Column(length = 150)
    private String lieu;

    /**
     * Nombre de membres présents ayant émargé.
     */
    @Column(name = "nombre_presents")
    private Integer nombrePresents;

    /**
     * Nombre de membres excusés / absents.
     */
    @Column(name = "nombre_absents")
    private Integer nombreAbsents;

    /**
     * Identifiant du document archivé dans la GED de la feuille de présence émargée.
     */
    @Column(name = "feuille_presence_ged_id")
    private UUID feuillePresenceGedId;

    /**
     * Identifiant du document archivé dans la GED du Procès-Verbal (PV) signé.
     */
    @Column(name = "pv_document_ged_id")
    private UUID pvDocumentGedId;

    /**
     * Synthèse des arbitrages et recommandations adoptés en séance.
     */
    @Column(columnDefinition = "TEXT")
    private String syntheseRecommandations;

    /**
     * Horodatage d'enregistrement.
     */
    @Column(name = "cree_le", nullable = false)
    @Builder.Default
    private LocalDateTime creeLe = LocalDateTime.now();
}
