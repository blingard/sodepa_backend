package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Requête pour une ligne d'écriture comptable.
 */
public record LigneRequest(
        @NotBlank String compteCode,
        UUID tiersId,
        BigDecimal debit,
        BigDecimal credit,
        String libelleLigne
) {}
