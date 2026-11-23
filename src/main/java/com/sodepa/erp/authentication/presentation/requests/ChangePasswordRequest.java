package com.sodepa.erp.authentication.presentation.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sodepa.erp.share.password.StrongPasswordSubject;

/**
 * Représente la requête de changement de mot de passe.
 */
public record ChangePasswordRequest (
    @JsonAlias("newPassword")
    String password,
    String currentPassword
) implements StrongPasswordSubject {
    public ChangePasswordRequest(String password) {
        this(password, null);
    }
}
