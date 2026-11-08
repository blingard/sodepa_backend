package com.sodepa.erp.utils;

/**
 * Cycle de vie normalisé des données du Référentiel SODEPA (CA-REF-001-05, F-REF-002, RG-REF-025).
 */
public enum StatutCycleVieReferentiel {
    BROUILLON,
    SOUMIS,
    EN_VALIDATION,
    ACTIF,
    PUBLIE,
    SUSPENDU,
    ARCHIVE
}
