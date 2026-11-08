package com.sodepa.erp.achats.infrastructure.entities;

/**
 * Entité de persistance représentant `StatutMarcheDossier` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum StatutMarcheDossier {
    BROUILLON,
    EN_ATTENTE_PROFORMA,
    SELECTION_PRESTATAIRE,
    ENGAGE_BUDGET,
    SIGNE,
    NOTIFIE,
    LIVRAISON_EN_COURS,
    RECEPTIONNE,
    LIQUIDE,
    SOLDE,
    ANNULE
}
