package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Données d'entrée pour une ligne de relevé manuel.
 */
public record LigneReleveInput(
    LocalDate dateTransaction,
    String libelle,
    BigDecimal montant
) {}
