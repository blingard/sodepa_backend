package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

public record KpiReportOutput(
    BigDecimal resultatNet,
    BigDecimal capitauxPropres,
    BigDecimal totalActif,
    BigDecimal roe,
    BigDecimal roa,
    BigDecimal ratioLiquiditeGenerale
) {}
