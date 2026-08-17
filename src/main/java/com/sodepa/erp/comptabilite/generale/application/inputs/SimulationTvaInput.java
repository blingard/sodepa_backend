package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.math.BigDecimal;

/**
 * Entrée pour la simulation de TVA.
 */
public record SimulationTvaInput(
        BigDecimal montantHt,
        BigDecimal tauxTva,
        String compteHtCode
) {}
