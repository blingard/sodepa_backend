package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération définissant les actions automatiques déclenchées en cas de dépassement
 * du délai limite de traitement convenu (SLA - Service Level Agreement) pour une tâche de workflow.
 * <p>
 * Ces mécanismes garantissent la continuité de service au sein de la SODEPA (ex: éviter les blocages
 * d'approvisionnement des ranchs ou de commandes urgentes d'abattoir).
 * </p>
 */
public enum ActionDepassementSla {

    /**
     * Aucune action automatisée n'est déclenchée lors du franchissement de l'échéance.
     * La tâche reste en attente sans notification corrective.
     */
    AUCUNE,

    /**
     * Envoi automatique d'un courriel de rappel/relance au valideur assigné ou au groupe concerné
     * pour signaler le retard de traitement.
     */
    RELANCE_EMAIL,

    /**
     * Escalade hiérarchique automatique vers le supérieur hiérarchique (N+1, Directeur de division ou DG)
     * pour arbitrage ou réassignation de la tâche en souffrance.
     */
    ESCALADE_MANAGER,

    /**
     * Rejet automatique du dossier pour caducité ou dépassement de délai critique,
     * entraînant la clôture en statut REJETE et la notification de l'initiateur.
     */
    REJET_AUTOMATIQUE
}
