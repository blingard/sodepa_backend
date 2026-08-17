package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

public record ReallocationInput(
    UUID sourceItemId,
    UUID destItemId,
    BigDecimal montant,
    UUID responsableId,
    String raison
) {}
