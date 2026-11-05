package com.sodepa.erp.authentication.presentation.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Représente la requête de déconnexion.
 */
public record LogoutRequest(
    @NotBlank(message = "REFRESH_TOKEN_NOT_VALID") String refreshToken
) {}
