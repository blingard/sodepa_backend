package com.sodepa.erp.authentication.presentation.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Représente la requête de rafraîchissement du jeton.
 */
public record RefreshRequest(
    @NotBlank String refreshToken
) {}
