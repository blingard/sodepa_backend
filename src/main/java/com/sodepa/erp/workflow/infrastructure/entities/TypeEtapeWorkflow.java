package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Types d'étapes possibles au sein d'un circuit de workflow.
 */
public enum TypeEtapeWorkflow {
    VALIDATION_HUMAINE,
    SIGNATURE_ELECTRONIQUE,
    CONTROLE_AUTO,
    NOTIFICATION
}
