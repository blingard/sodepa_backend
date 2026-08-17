package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.util.UUID;

/**
 * Données d'entrée pour la mise à jour d'un compte comptable.
 */
@Builder
public record UpdateCompteInput(
    @NotNull UUID id,
    @NotBlank String code,
    @NotBlank String intitule,
    String parentCode,
    @NotNull Integer niveau,
    String typeAnalytique,
    String nature,
    Boolean isAuxiliaire
) {
}
