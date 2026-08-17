package com.sodepa.erp.user.presentation.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

/**
 * Requête pour la mise à jour d'un utilisateur.
 */
public record UpdateUserRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @Email @NotBlank String email,
        @NotEmpty Set<String> telephones,
        @NotNull Boolean actif
) {
}
