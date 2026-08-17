package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

public record CadrageInput(
    int annee,
    String comptePrefix,
    BigDecimal coefficient,
    UUID responsableId
) {}
