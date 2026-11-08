package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CloturerAffectationInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record CloturerAffectationInput(
        UUID affectationId,
        LocalDate dateFin,
        String motifFin
) {}
