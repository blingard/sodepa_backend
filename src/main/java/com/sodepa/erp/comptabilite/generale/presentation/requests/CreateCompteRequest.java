package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Requête de création d'un compte comptable.
 */
@Builder
public record CreateCompteRequest(
    @NotBlank String code,
    @NotBlank String intitule,
    String parentCode,
    @NotNull Integer niveau,
    String typeAnalytique,
    String nature,
    Boolean isAuxiliaire
) {
}
