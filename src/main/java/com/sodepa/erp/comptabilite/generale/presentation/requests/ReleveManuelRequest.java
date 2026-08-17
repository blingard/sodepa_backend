package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Requête pour la saisie d'un relevé bancaire manuel.
 */
public record ReleveManuelRequest(
    @NotNull UUID banqueId,
    @NotNull LocalDate dateReleve,
    @NotNull BigDecimal soldeInitial,
    @NotNull BigDecimal soldeFinal,
    @NotEmpty List<LigneReleveRequest> lignes
) {}
