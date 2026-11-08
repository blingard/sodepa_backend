package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

/**
 * Dépôt Spring Data JPA pour l'accès aux données et la persistance de `RunwayReportOutput`.
 */
public record RunwayReportOutput(
    BigDecimal tresorerieDisponible,
    BigDecimal burnRateMensuelMoyen,
    BigDecimal runwayMois,
    String diagnostic
) {}
