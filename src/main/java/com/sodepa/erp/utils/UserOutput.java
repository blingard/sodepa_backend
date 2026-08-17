package com.sodepa.erp.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for {@link com.sodepa.erp.user.infrastructure.entities.UtilisateurEntity}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public record UserOutput(@NotNull UUID id, @NotNull @NotEmpty @NotBlank String username,
                         @NotNull @NotEmpty @NotBlank String nom, @NotNull @NotEmpty @NotBlank String prenom,
                         @NotNull @NotEmpty @NotBlank String email, String photoProfile, boolean actif,
                         Set<String> telephones, @NotNull Set<Permissions> permissions) implements Serializable {
}