package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Composant applicatif `RecommandationPaiementOutput` du système ERP SODEPA.
 */
public record RecommandationPaiementOutput(
    String libelleFacture,
    BigDecimal montant,
    LocalDate dateEcheance,
    Boolean aEscompte,
    String priorite,
    String actionProposee
) {}
