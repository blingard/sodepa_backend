package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;

public record CashFlowMensuelOutput(
    String mois,
    BigDecimal totalEncaissements,
    BigDecimal totalDecaissements,
    BigDecimal soldeNet,
    BigDecimal tresorerieCumulee
) {}
