package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.time.LocalDate;

/**
 * Données d'entrée pour la récupération du grand livre.
 */
public record GetGrandLivreInput(
    LocalDate debut,
    LocalDate fin
) {}
