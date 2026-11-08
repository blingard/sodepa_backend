package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

/**
 * Composant applicatif `CashFlowMensuelOutput` du système ERP SODEPA.
 */
public record CashFlowMensuelOutput(
    String mois,
    BigDecimal totalEncaissements,
    BigDecimal totalDecaissements,
    BigDecimal soldeNet,
    BigDecimal tresorerieCumulee
) {}
