package com.sodepa.erp.authentication.application.inputs;

/**
 * Données d'entrée pour le cas d'utilisation de déconnexion.
 */
public record LogoutInput(
    String refreshToken
) {}
