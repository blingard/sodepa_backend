package com.sodepa.erp.budget.presentation.requests;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `SaisirDemandeRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record SaisirDemandeRequest(
    @NotNull UUID departementId,
    @Positive int annee,
    @NotBlank String compteCode,
    UUID sectionId,
    @NotNull @Positive BigDecimal montant,
    String commentaires
) {}
