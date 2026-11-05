package com.sodepa.erp.authentication.presentation.requests;

import com.sodepa.erp.share.password.StrongPassword;
import com.sodepa.erp.share.password.StrongPasswordSubject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Représente la requête de connexion.
 */
public record LoginRequest(
    @Size(max = 10, min = 5, message = "USERNAME_LENGHT_NOT_VALID") String username,
    String password
)  implements StrongPasswordSubject {}
