package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Composant applicatif `BudgetDemandeOutput` du système ERP SODEPA.
 */
public record BudgetDemandeOutput(
    UUID id,
    UUID departementId,
    int annee,
    String compteCode,
    UUID sectionId,
    BigDecimal montantDemande,
    String statut,
    String commentaires
) {}
