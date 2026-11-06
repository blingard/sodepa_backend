package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération des types d'actions consignées dans le journal d'audit infalsifiable du moteur de workflow.
 * <p>
 * Chaque transition, changement d'état ou intervention humaine/système est horodatée
 * et tracée pour garantir la conformité aux normes OHADA et de contrôle interne de la SODEPA.
 * </p>
 */
public enum ActionWorkflowAudit {

    /**
     * Démarrage initial du circuit de validation par l'agent ou le service demandeur.
     */
    INITIATION,

    /**
     * Approbation explicite et validation de l'étape courante par un valideur autorisé.
     */
    APPROBATION,

    /**
     * Rejet définitif de la demande à l'étape courante, motivé par un commentaire obligatoire.
     */
    REJET,

    /**
     * Demande formelle de modifications ou de compléments d'information transmise à l'initiateur
     * (met l'instance en statut SUSPENDU en attente de révision).
     */
    DEMANDE_REVISION,

    /**
     * Délégation de la tâche à un collaborateur suppléant (ex: congé, mission sur le terrain dans un ranch).
     */
    DELEGATION,

    /**
     * Escalade hiérarchique consécutive au dépassement du délai de traitement SLA.
     */
    ESCALADE,

    /**
     * Clôture définitive du circuit de validation après franchissement réussi de la dernière étape
     * ou annulation administrative.
     */
    CLOTURE
}
