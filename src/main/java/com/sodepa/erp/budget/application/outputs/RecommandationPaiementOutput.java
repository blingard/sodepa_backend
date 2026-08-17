package com.sodepa.erp.budget.application.outputs;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommandationPaiementOutput(
    String libelleFacture,
    BigDecimal montant,
    LocalDate dateEcheance,
    Boolean aEscompte,
    String priorite,
    String actionProposee
) {}
