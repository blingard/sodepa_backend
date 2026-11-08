package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `MettreAJourRealiseObjectifInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record MettreAJourRealiseObjectifInput(
        UUID id,
        BigDecimal valeurRealisee
) {}
