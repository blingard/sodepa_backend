package com.sodepa.erp.tresorerie.regies.infrastructure.entities;

/**
 * Entité de persistance représentant `StatutApprovisionnementRegie` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum StatutApprovisionnementRegie {
    EN_ATTENTE_DAF,
    EN_ATTENTE_DG,
    APPROUVE_APPROVISIONNE,
    REJETE
}
