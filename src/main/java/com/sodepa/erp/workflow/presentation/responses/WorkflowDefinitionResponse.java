package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDomaine;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO) représentant un modèle de circuit de validation
 * et l'intégralité de ses étapes configurées avec leurs acteurs attendus.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinitionResponse {

    /** Identifiant unique universel du circuit. */
    private UUID id;

    /** Identifiant de l'opération métier associée le cas échéant. */
    private UUID metierId;

    /** Code de l'opération métier associée (ex: "RECRUITMENT"). */
    private String metierCode;

    /** Titre de l'opération métier associée. */
    private String metierTitle;

    /** Identifiant de l'unité organisationnelle propriétaire. */
    private UUID uniteOrganisationnelleId;

    /** Intitulé de l'unité organisationnelle propriétaire. */
    private String uniteOrganisationnelleNom;

    /** Code fonctionnel unique (ex: "WF-INFO-MEMO"). */
    private String code;

    /** Intitulé lisible du circuit. */
    private String intitule;

    /** Description détaillée des objectifs et règles du circuit. */
    private String description;

    /** Domaine fonctionnel (BUDGET, RH, GOUVERNANCE, etc.). */
    private WorkflowDomaine domaine;

    /** Numéro de version du circuit. */
    private Integer version;

    /** Indicateur d'état actif ou inactif. */
    private boolean actif;

    /** Rôles Keycloak habilités à initier le circuit (Maker). */
    private String rolesInitiateurs;

    /** Postes organisationnels habilités à initier le circuit (Maker). */
    private String postesInitiateurs;

    /** Liste ordonnée des étapes constitutives de ce workflow. */
    private List<WorkflowEtapeResponse> etapes;

    /**
     * DTO représentant une étape au sein de la réponse de définition de workflow.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkflowEtapeResponse {

        /** Identifiant unique de l'étape. */
        private UUID id;

        /** Numéro d'ordre séquentiel (1, 2, 3...). */
        private Integer ordre;

        /** Code technique de l'étape. */
        private String codeEtape;

        /** Intitulé lisible affiché à l'utilisateur. */
        private String intitule;

        /** Typologie d'étape (VALIDATION_HUMAINE, SIGNATURE_ELECTRONIQUE, etc.). */
        private String typeEtape;

        /** Règle de consensus (UN_SEUL_SUFFIT, TOUS_DOIVENT_VALIDER, MAJORITE). */
        private String politiqueValidation;

        /** Stratégie de désignation par défaut de l'approbateur. */
        private String strategieAssignation;

        /** Valeur de ciblage associée à la stratégie d'assignation. */
        private String valeurAssignation;

        /** Délai limite de traitement alloué en heures (SLA). */
        private Integer delaiSlaHeures;

        /** Indicateur d'étape active ou suspendue. */
        private boolean actif;

        /** Liste des acteurs attendus configurés sur cette étape. */
        private List<WorkflowEtapeActeurResponse> acteurs;
    }

    /**
     * DTO représentant un acteur / validateur configuré sur une étape.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkflowEtapeActeurResponse {

        /** Identifiant unique de la configuration de l'acteur. */
        private UUID id;

        /** Libellé de l'action attendue (ex: "Visa Budgétaire"). */
        private String intituleAction;

        /** Identifiant de l'employé nommé s'il est désigné. */
        private UUID employeId;

        /** Matricule de l'employé nommé. */
        private String employeMatricule;

        /** Nom complet de l'employé nommé. */
        private String employeNom;

        /** Identifiant de l'utilisateur technique s'il est renseigné. */
        private UUID utilisateurId;

        /** Nom complet de l'utilisateur technique. */
        private String utilisateurNom;

        /** Unité organisationnelle exigée pour cet acteur. */
        private UUID uniteOrganisationnelleId;

        /** Nom de l'unité organisationnelle exigée. */
        private String uniteOrganisationnelleNom;

        /** Stratégie d'assignation dynamique. */
        private String strategieAssignation;

        /** Valeur cible d'assignation (rôle, poste, etc.). */
        private String valeurAssignation;

        /** Indicateur d'obligation (true = REQUIRED, false = OPTIONAL). */
        private boolean obligatoire;

        /** Délai SLA spécifique en heures pour cet acteur. */
        private Integer delaiSlaHeures;

        /** Indicateur si ce validateur est actif ou désactivé. */
        private boolean actif;
    }
}
