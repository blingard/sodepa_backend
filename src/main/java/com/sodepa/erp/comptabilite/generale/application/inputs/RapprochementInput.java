package com.sodepa.erp.comptabilite.generale.application.inputs;

import java.util.UUID;

/**
 * Données d'entrée pour le rapprochement bancaire automatique.
 */
public record RapprochementInput(
    UUID releveId,
    String compteBanqueCode
) {}
