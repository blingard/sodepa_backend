package com.sodepa.erp.paie.infrastructure.entities;

/**
 * Entité de persistance représentant `ModePaiement` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum ModePaiement {
    VIREMENT,
    BILLETAGE,
    CHEQUE
}
