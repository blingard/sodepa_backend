package com.sodepa.erp.comptabilite.generale.application.outputs;

import java.math.BigDecimal;

/**
 * Ligne du plan d'amortissement d'une immobilisation.
 */
public record AmortissementLineOutput(
    int annee,
    BigDecimal baseAmortissable,
    BigDecimal dotation,
    BigDecimal amortissementsCumules,
    BigDecimal valeurNetteComptable
) {
}
