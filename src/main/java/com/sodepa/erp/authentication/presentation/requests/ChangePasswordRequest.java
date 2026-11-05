package com.sodepa.erp.authentication.presentation.requests;

import com.sodepa.erp.share.password.StrongPasswordSubject;

/**
 * Représente la requête de changement de mot de passe.
 */
public record ChangePasswordRequest (
    String password
) implements StrongPasswordSubject {}
