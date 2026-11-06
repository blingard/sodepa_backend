package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération des stratégies de résolution dynamique des approbateurs pour une étape donnée.
 * <p>
 * Permet au moteur de workflow de déterminer automatiquement qui doit valider en s'appuyant
 * soit sur l'organigramme hiérarchique SODEPA, soit sur le référentiel des postes et habilitations Keycloak.
 * </p>
 */
public enum StrategieAssignation {

    /**
     * Résolution du responsable hiérarchique direct (N+1) de l'initiateur du dossier
     * (ex: Chef de service ou Chef d'équipe).
     */
    MANAGER_N1,

    /**
     * Résolution du responsable hiérarchique au second niveau (N+2)
     * (ex: Directeur de division ou DAF pour un agent).
     */
    MANAGER_N2,

    /**
     * Résolution du responsable de l'unité opérationnelle rattachée (Chef de Ranch Faro/Jakiri/Dumbo,
     * Directeur de l'Abattoir de Yaoundé/Douala, Chef de Centre).
     */
    RESPONSABLE_STRUCTURE,

    /**
     * Attribution à tous les utilisateurs porteurs d'un rôle ou groupe Keycloak donné
     * (ex: "ROLE_DAF", "ROLE_CONTROLE_GESTION").
     */
    ROLE_KEYCLOAK,

    /**
     * Attribution selon le code officiel de la fiche de poste dans le référentiel RH
     * (ex: "PST-DIR-TECH", "PST-CHEF-COMPTABLE").
     */
    POSTE_SPECIFIQUE,

    /**
     * Attribution directe à un utilisateur fixe nommément identifié par son identifiant unique.
     */
    UTILISATEUR_FIXE
}
