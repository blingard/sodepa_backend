package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.comptabilite.generale.infrastructure.entities.CompteEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountOutput(@NotNull UUID id, @NotNull @NotEmpty @NotBlank String code,
                            @NotNull @NotEmpty @NotBlank String intitule, String parentCode,
                            @NotNull @Min(1) @Positive Integer niveau,
                            @NotNull @NotEmpty @NotBlank String typeAnalytique,
                            @NotNull @NotEmpty @NotBlank String nature,
                            @NotNull Boolean isAuxiliaire) implements Serializable {
}