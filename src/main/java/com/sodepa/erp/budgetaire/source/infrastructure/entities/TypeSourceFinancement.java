package com.sodepa.erp.budgetaire.source.infrastructure.entities;

/**
 * Origine des fonds d'une ligne budgétaire (spécification, § 4). Sans
 * contrainte CHECK en base : un type peut s'ajouter sans migration.
 */
public enum TypeSourceFinancement {

    /** Recettes propres de la SODEPA. */
    RESSOURCES_PROPRES,
    /** Subvention de l'État ou d'une collectivité. */
    SUBVENTION,
    /** Financement d'un partenaire ou bailleur, nommé sur la source (H-30). */
    PARTENAIRE
}
