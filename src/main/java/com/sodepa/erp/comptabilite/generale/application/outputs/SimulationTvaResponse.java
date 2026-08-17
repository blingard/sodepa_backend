package com.sodepa.erp.comptabilite.generale.application.outputs;

import java.math.BigDecimal;

/**
 * Réponse pour la simulation de TVA.
 */
public record SimulationTvaResponse(
        String compteHtCode,
        BigDecimal montantHt,
        String compteTvaCode,
        BigDecimal montantTva,
        String compteTiersCode,
        BigDecimal montantTtc
) {}
