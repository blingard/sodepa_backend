package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.DeplacerUniteInput;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `DeplacerUniteRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record DeplacerUniteRequest(
        UUID nouveauParentId
) {
    public DeplacerUniteInput toInput(UUID uniteId) {
        return DeplacerUniteInput.builder()
                .uniteId(uniteId)
                .nouveauParentId(nouveauParentId)
                .build();
    }
}
