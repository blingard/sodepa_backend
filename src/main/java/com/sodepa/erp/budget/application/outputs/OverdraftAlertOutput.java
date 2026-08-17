package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.util.UUID;

public record OverdraftAlertOutput(
    UUID ligneId,
    String intituleLigne,
    BigDecimal plafond,
    BigDecimal soldeUtilise,
    BigDecimal tauxUtilisation,
    String message
) {}
