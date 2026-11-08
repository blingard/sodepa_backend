package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `SaisirDemandeInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record SaisirDemandeInput(
    UUID departementId,
    int annee,
    String compteCode,
    UUID sectionId,
    BigDecimal montant,
    String commentaires
) {}
