package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Requête pour la synchronisation automatique d'un relevé bancaire.
 */
public record SyncRequest(
    @NotNull UUID banqueId,
    @NotNull LocalDate dateReleve,
    @NotNull BigDecimal soldeInitial
) {}
