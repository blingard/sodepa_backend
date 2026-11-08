package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CouvertureRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CouvertureRequest(
    @NotBlank String reference,
    @NotBlank String devise,
    @NotNull @Positive BigDecimal montantDevise,
    @NotNull @Positive BigDecimal coursGaranti,
    @NotNull LocalDate dateEffet,
    @NotNull LocalDate dateEcheance
) {}
