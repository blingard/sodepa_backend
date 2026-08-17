package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Requête pour une ligne de relevé bancaire manuel.
 */
public record LigneReleveRequest(
    @NotNull LocalDate dateTransaction,
    @NotBlank String libelle,
    @NotNull BigDecimal montant
) {}
