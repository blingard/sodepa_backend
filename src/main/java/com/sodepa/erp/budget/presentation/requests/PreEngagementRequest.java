package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `PreEngagementRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record PreEngagementRequest(
    @NotNull UUID planId,
    @NotBlank String compteCode,
    @NotNull UUID sectionId,
    @NotBlank String numeroEngagement,
    String description,
    @NotNull @Positive BigDecimal montant,
    @NotNull UUID utilisateurId
) {}
