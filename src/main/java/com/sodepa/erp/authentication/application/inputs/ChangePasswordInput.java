package com.sodepa.erp.authentication.application.inputs;

/**
 * Données d'entrée pour le cas d'utilisation de changement de mot de passe.
 */
public record ChangePasswordInput(
    String userId,
    String newPassword
) {}
