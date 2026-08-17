package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Données d'entrée pour la réévaluation.
 */
public record ReevaluationInput(
    int annee,
    Map<String, BigDecimal> coursCloture
) {}
