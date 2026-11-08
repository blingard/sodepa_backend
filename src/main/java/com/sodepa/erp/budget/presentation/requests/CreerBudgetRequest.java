package com.sodepa.erp.budget.presentation.requests;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreerBudgetRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreerBudgetRequest(
    @Positive int annee,
    @NotBlank String intitule,
    @NotNull UUID utilisateurId
) {}
