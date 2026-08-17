package com.sodepa.erp.budget.application.inputs;

import java.math.BigDecimal;
import java.util.UUID;

public record SaisirDemandeInput(
    UUID departementId,
    int annee,
    String compteCode,
    UUID sectionId,
    BigDecimal montant,
    String commentaires
) {}
