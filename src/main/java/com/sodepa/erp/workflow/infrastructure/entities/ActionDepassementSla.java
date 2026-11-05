package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Action automatique déclenchée en cas de dépassement du délai de traitement (SLA).
 */
public enum ActionDepassementSla {
    AUCUNE,
    RELANCE_EMAIL,
    ESCALADE_MANAGER,
    REJET_AUTOMATIQUE
}
