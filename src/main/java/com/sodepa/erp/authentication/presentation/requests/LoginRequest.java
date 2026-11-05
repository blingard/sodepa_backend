package com.sodepa.erp.authentication.presentation.requests;

import com.sodepa.erp.share.password.StrongPasswordSubject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Représente la requête de connexion.
 */
public record LoginRequest(
    @NotBlank(message = "USERNAME_LENGHT_NOT_VALID")
    @Size(min = 3, max = 150, message = "USERNAME_LENGHT_NOT_VALID")
    String username,
    @NotBlank(message = "PASSWORD_LENGHT_NOT_VALID")
    String password
) implements StrongPasswordSubject {}
