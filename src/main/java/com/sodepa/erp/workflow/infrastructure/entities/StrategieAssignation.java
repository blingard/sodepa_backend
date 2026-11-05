package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Stratégies de résolution des approbateurs via l'organigramme ou les rôles.
 */
public enum StrategieAssignation {
    MANAGER_N1,
    MANAGER_N2,
    RESPONSABLE_STRUCTURE,
    ROLE_KEYCLOAK,
    POSTE_SPECIFIQUE,
    UTILISATEUR_FIXE
}
