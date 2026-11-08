package com.sodepa.erp.formation.infrastructure.entities;

/**
 * Entité de persistance représentant `StatutStage` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum StatutStage {
    EN_COURS,
    RALLONGE_DEMANDEE,
    RALLONGE_ACCORDEE,
    TERMINE,
    REJETE
}
