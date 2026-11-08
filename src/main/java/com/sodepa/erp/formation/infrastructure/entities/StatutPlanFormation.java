package com.sodepa.erp.formation.infrastructure.entities;

/**
 * Entité de persistance représentant `StatutPlanFormation` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum StatutPlanFormation {
    BROUILLON,
    SOUMIS_DAF,
    VALIDE_DG,
    EN_COURS,
    CLOTURE
}
