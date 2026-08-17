package com.sodepa.erp.authentication.presentation.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Représente la requête de déconnexion.
 */
public record LogoutRequest(
    @NotBlank String refreshToken
) {}
