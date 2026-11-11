package com.sodepa.erp.budget.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant le Groupe de Travail Budgétaire (GT) constitué par décision du Directeur Général
 * (Cahier des charges BUD - EP-BUD-02, F-BUD-005 : Groupe de travail budgétaire).
 */
@Entity
@Table(name = "budget_groupe_travail")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetGroupeTravailEntity {
    /**
     * Préfixe du code d'identification métier, non persisté en base de données.
     */
    @Transient
    @Builder.Default
    private String codePrefix = "GT-BUD";

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
     * Référence à la campagne budgétaire rattachée.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campagne_id", nullable = false)
    private BudgetCampagneEntity campagne;

    /**
     * Référence de la décision du DG créant le groupe de travail (ex: DEC-2026-042/SODEPA/DG).
     */
    @Column(name = "decision_creation_ref", nullable = false, length = 100)
    private String decisionCreationRef;

    /**
     * Date de signature de la décision de création.
     */
    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation;

    /**
     * Président du Groupe de Travail (ex: DAF).
     */
    @Column(name = "president_titre", length = 150)
    private String presidentTitre;

    /**
     * Rapporteur du Groupe de Travail (ex: Chef Service du Budget / Chef CSPP).
     */
    @Column(name = "rapporteur_titre", length = 150)
    private String rapporteurTitre;

    /**
     * Composition et liste des membres désignés.
     */
    @Column(columnDefinition = "TEXT")
    private String compositionMembres;

    /**
     * Identifiant du document archivé dans la GED de la décision de création signée.
     */
    @Column(name = "decision_ged_id")
    private UUID decisionGedId;

    /**
     * Statut du groupe de travail (ACTIF, CLOTURE).
     */
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String statut = "ACTIF";

    /**
     * Horodatage de création.
     */
    @Column(name = "cree_le", nullable = false)
    @Builder.Default
    private LocalDateTime creeLe = LocalDateTime.now();
}
