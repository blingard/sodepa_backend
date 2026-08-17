package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

public record PreEngagementInput(
    UUID planId,
    String compteCode,
    UUID sectionId,
    String numeroEngagement,
    String description,
    BigDecimal montant,
    UUID utilisateurId
) {}
