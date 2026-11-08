package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `TftOhadaReportOutput`.
 */
public record TftOhadaReportOutput(
    Integer annee,
    BigDecimal fluxActivitesOperationnelles,
    BigDecimal fluxActivitesInvestissement,
    BigDecimal fluxActivitesFinancement,
    BigDecimal variationNetTresorerie,
    BigDecimal tresorerieDebutExercice,
    BigDecimal tresorerieFinExercice
) {}
