package com.sodepa.erp.pastoral.infrastructure.entities;

/**
 * Entité de persistance représentant `TypeMouvementCheptel` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum TypeMouvementCheptel {
    NAISSANCE,
    PESEE_CROISSANCE,
    CHANGEMENT_CATEGORIE,
    TRANSFERT_RANCH,
    MORTALITE,
    VENTE_ABATTAGE,
    REFORME
}
