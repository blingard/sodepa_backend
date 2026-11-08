package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerPrevisionRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerPrevisionRequest(
    @NotNull LocalDate dateEcheance,
    @NotBlank String type,
    @NotBlank String source,
    @NotBlank String libelle,
    @NotNull @Positive BigDecimal montant
) {}
