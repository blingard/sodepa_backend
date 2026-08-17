package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Données d'entrée pour un relevé manuel.
 */
public record ReleveManuelInput(
    UUID banqueId,
    LocalDate dateReleve,
    BigDecimal soldeInitial,
    BigDecimal soldeFinal,
    List<LigneReleveInput> lignes
) {}
