package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

public record ValiderEtapeInput(
    String numeroEngagement,
    String roleApprobateur,
    UUID utilisateurId
) {}
