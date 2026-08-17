package com.sodepa.erp.comptabilite.analytique.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entité représentant une ligne de budget prévisionnel annuel.
 * Permet de définir les objectifs financiers (charges/produits) par section analytique et par compte général.
 */
@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(name = "uq_budget_period_section_account", columnNames = {"annee", "section_id", "compte_code"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetEntity {

    /**
     * Identifiant unique de la ligne de budget.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Exercice budgétaire concerné (ex: 2026).
     */
    @Column(nullable = false)
    private Integer annee;

    /**
     * Section analytique sur laquelle porte le budget.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = false)
    private SectionAnalytiqueEntity section;

    /**
     * Code du compte général ciblé par la ligne budgétaire (ex: '605200' pour électricité).
     */
    @Column(name = "compte_code", nullable = false, length = 20)
    private String compteCode;

    /**
     * Montant budgété ou prévisionnel pour cet exercice.
     */
    @Column(name = "montant_budget", nullable = false, precision = 19, scale = 4)
    private BigDecimal montantBudget;
}
