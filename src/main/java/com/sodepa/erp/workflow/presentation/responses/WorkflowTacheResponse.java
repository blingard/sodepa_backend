package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.WorkflowDecision;
import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowTache;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO) représentant une tâche d'approbation individuelle.
 * Alimente notamment la corbeille de tâches / to-do list des valideurs dans l'ERP SODEPA.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTacheResponse {

    /** Identifiant unique de la tâche. */
    private UUID id;

    /** Identifiant de l'instance de workflow dont relève cette tâche. */
    private UUID instanceId;

    /** Identifiant de la définition de l'acteur d'étape lié. */
    private UUID acteurDefinitionId;

    /** Intitulé précis de l'action attendue (ex: "Visa Budgétaire DAF"). */
    private String intituleAction;

    /** Poids de l'action : true = obligatoire (bloquant), false = consultatif/optionnel. */
    private boolean obligatoire;

    /** Type de l'entité métier ciblée (ex: "ENGAGEMENT_BUDGETAIRE", "ORDRE_MISSION", "EMPLOYE"). */
    private String entityName;

    /** Clé primaire de l'entité métier ciblée. */
    private String entityId;

    /** Code technique de l'étape de validation. */
    private String etapeCode;

    /** Intitulé lisible de l'étape. */
    private String etapeIntitule;

    /** Identifiant de l'employé assigné (Clé métier centrale). */
    private UUID assigneAEmployeId;

    /** Matricule de l'employé assigné. */
    private String assigneAEmployeMatricule;

    /** Nom complet de l'employé assigné. */
    private String assigneAEmployeNom;

    /** Intitulé du poste de l'employé assigné. */
    private String assigneAEmployePoste;

    /** Identifiant du compte utilisateur assigné à la tâche (Liaison technique). */
    private UUID assigneAId;

    /** Nom et prénom complets du compte utilisateur assigné. */
    private String assigneANom;

    /** Statut actuel de la tâche (EN_ATTENTE, APPROUVEE, REJETEE, DELEGUEE, EXPIREE). */
    private StatutWorkflowTache statut;

    /** Décision prise par l'approbateur (APPROUVE, REJETE, DEMANDE_REVISION). */
    private WorkflowDecision decision;

    /** Commentaire ou motivation déposée par l'approbateur. */
    private String commentaire;

    /** Horodatage de création et d'attribution de la tâche. */
    private Instant dateCreation;

    /** Date et heure limites de traitement selon le délai SLA. */
    private Instant dateEcheance;

    /** Horodatage effectif de soumission de la décision. */
    private Instant dateDecision;

    /** Liste des champs autorisés à la modification. */
    private String champsModifiablesAutorises;
}
