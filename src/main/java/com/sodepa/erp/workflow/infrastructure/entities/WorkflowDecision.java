package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération des décisions formelles qu'un approbateur peut émettre lors du traitement
 * d'une tâche de validation dans le moteur universel SODEPA.
 */
public enum WorkflowDecision {

    /**
     * Approbation favorable : le valideur accorde son visa ou sa signature, permettant
     * le passage à l'étape suivante ou la clôture finale du dossier.
     */
    APPROUVED,

    /**
     * Rejet défavorable : le valideur refuse la demande, stoppant immédiatement le circuit.
     * Un motif textuel explicatif est généralement exigé.
     */
    REJETE,

    /**
     * Demande d'ajustements : le dossier n'est ni validé ni rejeté de façon définitive,
     * mais renvoyé à l'initiateur pour apporter des pièces jointes ou corrections requises.
     */
    ASK_REVISION
}
