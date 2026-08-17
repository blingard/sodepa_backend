package com.sodepa.erp.authentication.application.inputs;

/**
 * Données d'entrée pour le cas d'utilisation de connexion.
 */
public record LoginInput(
    String username,
    String password
) {}
