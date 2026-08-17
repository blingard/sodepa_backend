package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.time.LocalDate;

/**
 * Données d'entrée pour la récupération du livre journal.
 */
public record GetLivreJournalInput(
    LocalDate debut,
    LocalDate fin
) {}
