package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

public record RunwayReportOutput(
    BigDecimal tresorerieDisponible,
    BigDecimal burnRateMensuelMoyen,
    BigDecimal runwayMois,
    String diagnostic
) {}
