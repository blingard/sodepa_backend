package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

public record TftOhadaReportOutput(
    Integer annee,
    BigDecimal fluxActivitesOperationnelles,
    BigDecimal fluxActivitesInvestissement,
    BigDecimal fluxActivitesFinancement,
    BigDecimal variationNetTresorerie,
    BigDecimal tresorerieDebutExercice,
    BigDecimal tresorerieFinExercice
) {}
