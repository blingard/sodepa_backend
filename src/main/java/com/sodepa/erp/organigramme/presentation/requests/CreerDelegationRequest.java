package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.CreerDelegationInput;
import com.sodepa.erp.organigramme.infrastructure.entities.TypeDelegation;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreerDelegationRequest(
        @NotNull UUID delegantId,
        @NotNull UUID delegataireId,
        @NotNull TypeDelegation typeDelegation,
        BigDecimal plafondMontant,
        @NotNull Instant dateDebut,
        @NotNull Instant dateFin
) {
    public CreerDelegationInput toInput() {
        return CreerDelegationInput.builder()
                .delegantId(delegantId)
                .delegataireId(delegataireId)
                .typeDelegation(typeDelegation)
                .plafondMontant(plafondMontant)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .build();
    }
}
