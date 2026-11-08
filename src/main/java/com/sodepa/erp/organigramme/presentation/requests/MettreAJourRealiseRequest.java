package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.MettreAJourRealiseObjectifInput;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `MettreAJourRealiseRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record MettreAJourRealiseRequest(
        @NotNull BigDecimal valeurRealisee
) {
    public MettreAJourRealiseObjectifInput toInput(UUID objectifId) {
        return MettreAJourRealiseObjectifInput.builder()
                .id(objectifId)
                .valeurRealisee(valeurRealisee)
                .build();
    }
}
