package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Données d'entrée pour la création d'un compte comptable.
 */
@Builder
public record CreateCompteInput(
    @NotBlank String code,
    @NotBlank String intitule,
    String parentCode,
    @NotNull Integer niveau,
    String typeAnalytique,
    String nature,
    Boolean isAuxiliaire
) {
}
