package com.sodepa.erp.user.presentation.requests;

import com.sodepa.erp.utils.Permissions;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * Requête pour la création d'un utilisateur.
 */
public record CreateUserRequest(
        @NotBlank String username,
        @NotBlank String nom,
        @NotBlank String prenom,
        @Email @NotBlank String email,
        @NotEmpty Set<String> telephones,
        @NotEmpty Set<Permissions> permissions
) {
}
