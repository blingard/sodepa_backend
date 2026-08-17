package com.sodepa.erp.user.infrastructure.event;

import com.sodepa.erp.utils.Permissions;
import java.util.Set;
import java.util.UUID;

/**
 * Payload d'événement pour l'utilisateur.
 */
public record UserEventInput(
        UUID id,
        String username,
        String nom,
        String prenom,
        String email,
        String photoProfile,
        Set<String> telephones,
        Set<Permissions> permissions,
        boolean actif,
        String userId
) {
}
