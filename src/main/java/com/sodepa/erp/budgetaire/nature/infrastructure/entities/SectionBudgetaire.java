package com.sodepa.erp.budgetaire.nature.infrastructure.entities;

/** Section du budget à laquelle appartient une nature (spécification, § 4). */
public enum SectionBudgetaire {

    /** Charges courantes : achats, services, personnel. */
    FONCTIONNEMENT,
    /** Acquisitions d'immobilisations et leur financement. */
    INVESTISSEMENT,
    /** Recettes de l'activité : ventes, prestations, subventions d'exploitation. */
    PRODUITS
}
