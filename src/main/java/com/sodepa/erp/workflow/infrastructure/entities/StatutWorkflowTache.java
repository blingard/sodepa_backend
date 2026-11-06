package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération des statuts possibles d'une tâche individuelle assignée à un valideur au sein d'une étape de workflow.
 */
public enum StatutWorkflowTache {

    /**
     * La tâche est active et attend l'intervention (visa, signature ou rejet) de l'agent assigné.
     */
    EN_ATTENTE,

    /**
     * L'agent assigné a traité favorablement la tâche avec une décision d'approbation.
     */
    APPROUVEE,

    /**
     * L'agent assigné a refusé la validation de cette tâche en spécifiant un motif.
     */
    REJETEE,

    /**
     * La responsabilité du traitement de cette tâche a été déléguée à un suppléant.
     */
    DELEGUEE,

    /**
     * Le délai imparti (SLA) a expiré sans intervention, rendant la tâche caduque selon la politique configurée.
     */
    EXPIREE
}
