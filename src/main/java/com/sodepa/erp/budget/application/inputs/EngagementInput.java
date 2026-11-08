package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `EngagementInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record EngagementInput(
    UUID planId,
    String compteCode,
    UUID sectionId,
    String numeroEngagement,
    String description,
    BigDecimal montant,
    UUID utilisateurId
) {}
