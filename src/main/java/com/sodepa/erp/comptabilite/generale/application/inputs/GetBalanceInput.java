package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.time.LocalDate;

/**
 * Données d'entrée pour la récupération de la balance.
 */
public record GetBalanceInput(
    LocalDate debut,
    LocalDate fin
) {}
