package com.sodepa.erp.workflow.presentation.responses;

import com.sodepa.erp.workflow.infrastructure.entities.StatutWorkflowInstance;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO) représentant l'état d'exécution complet d'une instance de workflow.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowInstanceResponse {

    /** Identifiant unique de l'instance d'exécution. */
    private UUID id;

    /** Identifiant du modèle de définition de circuit parent. */
    private UUID definitionId;

    /** Code standardisé du circuit parent (ex: "WF-INFO-MEMO"). */
    private String definitionCode;

    /** Intitulé lisible du circuit parent. */
    private String definitionIntitule;

    /** Unité organisationnelle émettrice. */
    private UUID uniteOrganisationnelleId;

    /** Nom de l'unité organisationnelle émettrice. */
    private String uniteOrganisationnelleNom;

    /** Nom de l'entité métier ciblée (ex: "EMPLOYE", "NOTE_SERVICE", "ENGAGEMENT"). */
    private String entityName;

    /** Identifiant technique de l'entité métier ciblée. */
    private String entityId;

    /** Identifiant de l'employé initiateur (Maker). */
    private UUID initiateurEmployeId;

    /** Matricule de l'employé initiateur (ex: "SDP-2026-0001"). */
    private String initiateurEmployeMatricule;

    /** Nom complet de l'employé initiateur. */
    private String initiateurEmployeNom;

    /** Identifiant du compte utilisateur de l'initiateur. */
    private UUID initiateurId;

    /** Nom complet de l'utilisateur initiateur. */
    private String initiateurNom;

    /** Statut global d'avancement de l'instance (EN_COURS, APPROUVE, REJETE, SUSPENDU, ANNULE). */
    private StatutWorkflowInstance statut;

    /** Code de l'étape courante active (ex: "VISA_DAF"). */
    private String etapeCouranteCode;

    /** Intitulé de l'étape courante active. */
    private String etapeCouranteIntitule;

    /** Horodatage du déclenchement du circuit. */
    private Instant dateInitiation;

    /** Horodatage de la clôture définitive le cas échéant. */
    private Instant dateCloture;

    /** Liste ordonnée de toutes les tâches associées à cette instance (traitées ou en attente). */
    private List<WorkflowTacheResponse> taches;
}
