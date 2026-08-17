package com.sodepa.erp.user.application.inputs;

import org.springframework.data.domain.Pageable;

/**
 * Données d'entrée pour la recherche d'utilisateurs.
 */
public record SearchUsersInput(
        String nom,
        String prenom,
        String email,
        String telephone,
        Pageable pageable
) {
}
