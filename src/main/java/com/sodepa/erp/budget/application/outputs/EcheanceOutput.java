package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Composant applicatif `EcheanceOutput` du système ERP SODEPA.
 */
public record EcheanceOutput(
    UUID id,
    LocalDate dateEcheance,
    BigDecimal principal,
    BigDecimal interets,
    BigDecimal soldeRestantDu,
    String statut
) {}
