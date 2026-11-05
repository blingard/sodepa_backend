package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Types d'actions consignées dans la piste d'audit du workflow.
 */
public enum ActionWorkflowAudit {
    INITIATION,
    APPROBATION,
    REJET,
    DEMANDE_REVISION,
    DELEGATION,
    ESCALADE,
    CLOTURE
}
