package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sodepa.erp.utils.TypeTiers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.comptabilite.generale.infrastructure.entities.TiersEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TiersOutput(@NotNull UUID id, @NotNull @NotEmpty @NotBlank String code,
                          @NotNull @NotEmpty @NotBlank String raisonSociale,
                          @NotNull @NotEmpty @NotBlank String adresse, @NotNull @NotEmpty @NotBlank String telephone,
                          @Email String email, @NotNull TypeTiers typeTiers, @NotNull Boolean actif,
                          @NotNull @NotEmpty @NotBlank String compteCollectifCode) implements Serializable {
}