package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ReallocationRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record ReallocationRequest(
    @NotNull UUID sourceItemId,
    @NotNull UUID destItemId,
    @NotNull @Positive BigDecimal montant,
    @NotNull UUID responsableId,
    @NotBlank String raison
) {}
