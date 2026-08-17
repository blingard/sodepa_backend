package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.util.UUID;

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
