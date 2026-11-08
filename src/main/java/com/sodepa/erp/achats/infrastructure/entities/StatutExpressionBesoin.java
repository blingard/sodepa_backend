package com.sodepa.erp.achats.infrastructure.entities;

/**
 * Entité de persistance représentant `StatutExpressionBesoin` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum StatutExpressionBesoin {
    BROUILLON,
    VALIDEE_HIERARCHIE,
    CARENCE_CONSTATEE,
    VISEE_BUDGET,
    APPROUVEE_DG,
    REJETEE
}
