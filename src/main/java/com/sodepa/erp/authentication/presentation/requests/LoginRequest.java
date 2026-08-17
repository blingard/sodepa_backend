package com.sodepa.erp.authentication.presentation.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Représente la requête de connexion.
 */
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
