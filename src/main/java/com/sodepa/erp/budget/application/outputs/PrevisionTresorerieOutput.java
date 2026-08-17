package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrevisionTresorerieOutput(
    UUID id,
    LocalDate dateEcheance,
    String type,
    String source,
    String libelle,
    BigDecimal montant
) {}
