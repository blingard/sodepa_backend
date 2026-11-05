package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Statuts possibles d'une exécution de circuit de workflow.
 */
public enum StatutWorkflowInstance {
    EN_COURS,
    APPROUVE,
    REJETE,
    SUSPENDU,
    ANNULE
}
