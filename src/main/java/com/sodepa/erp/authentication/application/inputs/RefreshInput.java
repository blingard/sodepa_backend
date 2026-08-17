package com.sodepa.erp.authentication.application.inputs;

/**
 * Données d'entrée pour le cas d'utilisation de rafraîchissement.
 */
public record RefreshInput(
    String refreshToken
) {}
