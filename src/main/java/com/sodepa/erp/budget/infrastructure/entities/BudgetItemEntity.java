package com.sodepa.erp.budget.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sodepa.erp.comptabilite.analytique.infrastructure.entities.SectionAnalytiqueEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité représentant une ligne de budget détaillée pour un compte comptable et une section analytique.
 */
@Entity
@Table(name = "budget_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetItemEntity {

    /**
     * Identifiant unique de la ligne budgétaire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Plan budgétaire annuel auquel est rattaché cette ligne.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_plan_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private BudgetPlanEntity budgetPlan;

    /**
     * Code du compte comptable général (classe 6 ou 7).
     */
    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    /**
     * Identifiant de la section analytique (centre de coût, projet) associée, si applicable.
     */
    @Column(name = "section_id")
    private UUID sectionId;

    /**
     * Montant initial de l'enveloppe budgétaire annuelle.
     */
    @Column(name = "montant_annuel", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantAnnuel;

    /**
     * Montant planifié ajusté (après réallocations éventuelles).
     */
    @Column(name = "montant_planned", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantPlanned;

    /**
     * Cumul des montants de dépenses engagées mais non encore facturées.
     */
    @Column(name = "montant_engage", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantEngage;

    /**
     * Cumul des montants réels consommés et imputés via des écritures comptables.
     */
    @Column(name = "montant_real", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantReal;

    /**
     * Répartition périodique (mensuelle ou trimestrielle) de l'enveloppe.
     */
    @OneToMany(mappedBy = "budgetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<BudgetItemPeriodeEntity> periodes = new ArrayList<>();

    /**
     * Engagements budgétaires de dépenses liés à cette ligne.
     */
    @OneToMany(mappedBy = "budgetItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    private List<BudgetEngagementEntity> engagements = new ArrayList<>();

    /**
     * Associe un découpage périodique.
     * 
     * @param periode la période budgétaire
     */
    public void addPeriode(BudgetItemPeriodeEntity periode) {
        if (this.periodes == null) {
            this.periodes = new ArrayList<>();
        }
        this.periodes.add(periode);
        periode.setBudgetItem(this);
    }

    /**
     * Associe un engagement budgétaire.
     * 
     * @param engagement l'engagement de dépenses
     */
    public void addEngagement(BudgetEngagementEntity engagement) {
        if (this.engagements == null) {
            this.engagements = new ArrayList<>();
        }
        this.engagements.add(engagement);
        engagement.setBudgetItem(this);
    }
}
