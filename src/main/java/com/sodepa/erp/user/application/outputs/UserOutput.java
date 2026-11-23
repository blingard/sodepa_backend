package com.sodepa.erp.user.application.outputs;

import com.sodepa.erp.utils.Permissions;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sortie de données pour un utilisateur.
 */
public record UserOutput(
        UUID id,
        String username,
        String nom,
        String prenom,
        String email,
        String photoProfile,
        boolean actif,
        Set<String> telephones,
        Set<Permissions> permissions,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) {
    public UserOutput(
            UUID id,
            String username,
            String nom,
            String prenom,
            String email,
            String photoProfile,
            boolean actif,
            Set<String> telephones,
            Set<Permissions> permissions
    ) {
        this(id, username, nom, prenom, email, photoProfile, actif, telephones, permissions,
                false, false, null, null, null, null);
    }
}
