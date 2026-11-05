package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Règle de décision lorsque plusieurs personnes sont sollicitées sur une étape.
 */
public enum PolitiqueValidation {
    UN_SEUL_SUFFIT,
    TOUS_DOIVENT_VALIDER,
    MAJORITE
}
