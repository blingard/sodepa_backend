package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

public record CreerBudgetPlanInput(
    int annee,
    String intitule,
    UUID utilisateurId
) {}
