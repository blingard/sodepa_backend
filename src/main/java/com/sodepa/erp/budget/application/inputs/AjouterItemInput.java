package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

public record AjouterItemInput(
    UUID planId,
    String compteCode,
    UUID sectionId,
    BigDecimal montant
) {}
