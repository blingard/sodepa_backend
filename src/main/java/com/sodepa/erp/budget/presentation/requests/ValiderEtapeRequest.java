package com.sodepa.erp.budget.presentation.requests;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ValiderEtapeRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record ValiderEtapeRequest(
    @NotBlank String numeroEngagement,
    @NotBlank String roleApprobateur,
    @NotNull UUID utilisateurId
) {}
