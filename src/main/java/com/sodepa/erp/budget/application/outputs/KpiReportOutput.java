package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `KpiReportOutput`.
 */
public record KpiReportOutput(
    BigDecimal resultatNet,
    BigDecimal capitauxPropres,
    BigDecimal totalActif,
    BigDecimal roe,
    BigDecimal roa,
    BigDecimal ratioLiquiditeGenerale
) {}
