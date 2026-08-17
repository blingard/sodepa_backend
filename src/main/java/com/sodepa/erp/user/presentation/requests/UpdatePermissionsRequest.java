package com.sodepa.erp.user.presentation.requests;

import com.sodepa.erp.utils.Permissions;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

/**
 * Requête pour la mise à jour des permissions.
 */
public record UpdatePermissionsRequest(
        @NotEmpty Set<Permissions> permissions
) {
}
