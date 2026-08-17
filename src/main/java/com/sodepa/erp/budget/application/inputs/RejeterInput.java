package com.sodepa.erp.budget.application.inputs;

import java.util.UUID;

public record RejeterInput(
    String numeroEngagement,
    String motif,
    UUID utilisateurId
) {}
