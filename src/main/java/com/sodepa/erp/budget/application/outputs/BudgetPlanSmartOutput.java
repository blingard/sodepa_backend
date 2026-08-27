package com.sodepa.erp.budget.application.outputs;

import com.sodepa.erp.budget.infrastructure.entities.StatutBudget;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Plan budgétaire vu depuis la liste.
 *
 * <p>
 * Sans ses postes, à la différence de {@link BudgetPlanOutput} : une liste
 * d'exercices n'a pas à traîner toutes les lignes budgétaires de chacun. Le
 * décompte suffit à savoir si un plan est vide, et la fiche complète se
 * demande par {@code GET /budget/plans/{planId}}.
 * </p>
 */
public record BudgetPlanSmartOutput(
    UUID id,
    int annee,
    String intitule,
    int version,
    StatutBudget statut,
    BigDecimal totalBudget,
    int nombreItems,
    LocalDateTime creeLe,
    UUID creePar,
    LocalDateTime modifieLe,
    UUID modifiePar
) {}
