package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Données d'entrée pour la synchronisation automatique d'un relevé bancaire.
 */
public record SyncInput(
    UUID banqueId,
    LocalDate dateReleve,
    BigDecimal soldeInitial
) {}
