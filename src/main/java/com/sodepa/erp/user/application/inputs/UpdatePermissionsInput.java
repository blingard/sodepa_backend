package com.sodepa.erp.user.application.inputs;

import com.sodepa.erp.utils.Permissions;
import java.util.Set;
import java.util.UUID;

/**
 * Données d'entrée pour mettre à jour les permissions.
 */
public record UpdatePermissionsInput(
        UUID id,
        Set<Permissions> permissions
) {
}
