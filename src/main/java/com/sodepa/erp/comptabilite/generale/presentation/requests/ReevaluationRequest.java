package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Requête pour la réévaluation des comptes en devises.
 */
public record ReevaluationRequest(
    @NotNull Integer annee,
    @NotNull Map<String, BigDecimal> coursCloture
) {}
