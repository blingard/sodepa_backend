package com.sodepa.erp.budget.infrastructure.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité représentant la ventilation d'un budget par période (mensuelle ou trimestrielle).
 */
@Entity
@Table(name = "budget_item_periode")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetItemPeriodeEntity {

    /**
     * Identifiant unique de la période budgétaire.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Ligne budgétaire d'origine.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_item_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private BudgetItemEntity budgetItem;

    /**
     * Numéro de la période (ex: 1 à 12 pour les mois, ou 1 à 4 pour les trimestres).
     */
    @Column(name = "periode_num", nullable = false)
    private Integer periodeNum;

    /**
     * Montant budgété prévu pour cette période.
     */
    @Column(name = "montant_planned", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantPlanned;

    /**
     * Montant réel cumulé consommé sur cette période.
     */
    @Column(name = "montant_real", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantReal;
}
