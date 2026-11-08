package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `DeplacerUniteInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record DeplacerUniteInput(
        UUID uniteId,
        UUID nouveauParentId
) {}
