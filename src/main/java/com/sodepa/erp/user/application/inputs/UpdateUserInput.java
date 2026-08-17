package com.sodepa.erp.user.application.inputs;

import java.util.Set;
import java.util.UUID;

/**
 * Données d'entrée pour la mise à jour d'un utilisateur.
 */
public record UpdateUserInput(
        UUID id,
        String nom,
        String prenom,
        String email,
        Set<String> telephones,
        boolean actif
) {
}
