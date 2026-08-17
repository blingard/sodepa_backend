package com.sodepa.erp.authentication.presentation.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Représente la requête de changement de mot de passe.
 */
public record ChangePasswordRequest(
    @NotBlank String newPassword
) {}
