package com.sodepa.erp.budget.infrastructure.entities;

/**
 * Représente les différents statuts du cycle de vie d'un plan budgétaire.
 */
public enum StatutBudget {
    /**
     * Budget en cours d'élaboration (modifiable).
     */
    DRAFT,

    /**
     * Budget soumis pour validation au responsable ou directeur financier.
     */
    SUBMITTED,

    /**
     * Budget validé et publié (figé pour exécution).
     */
    PUBLISHED,

    /**
     * Budget rejeté lors du workflow de validation (retourne à l'état de brouillon).
     */
    REJECTED
}
