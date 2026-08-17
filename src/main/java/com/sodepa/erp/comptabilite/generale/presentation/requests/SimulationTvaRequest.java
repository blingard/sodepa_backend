package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Requête pour la simulation de TVA.
 */
public record SimulationTvaRequest(
        @NotNull BigDecimal montantHt,
        @NotNull BigDecimal tauxTva,
        @NotBlank String compteHtCode
) {}
