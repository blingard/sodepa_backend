package com.sodepa.erp.budget.infrastructure.entities;

/**
 * Représente les différents statuts du cycle de vie d'un plan budgétaire
 * conformément à la procédure officielle SODEPA (Section 9 du CdC).
 * 
 * Cycles successifs : En préparation → Voté → Exécutoire → Clôturé.
 * Versions : Brouillon → Soumise → Arbitrée → Validée DG → Approuvée CA → Rectifiée.
 */
public enum StatutBudget {
    /**
     * Budget en cours d'élaboration (modifiable).
     */
    DRAFT,
    EN_PREPARATION,

    /**
     * Budget soumis pour consolidation et arbitrage.
     */
    SUBMITTED,
    SOUMIS,

    /**
     * Budget arbitré par les instances techniques et de direction.
     */
    ARBITRE,

    /**
     * Budget validé par le Directeur Général pour présentation au CA.
     */
    VALIDE_DG,

    /**
     * Budget voté et approuvé par le Conseil d'Administration.
     */
    VOTE,
    APPROUVE_CA,

    /**
     * Budget rendu exécutoire par décision du Directeur Général (figé pour exécution).
     */
    PUBLISHED,
    EXECUTOIRE,

    /**
     * Budget rejeté lors du cycle de validation.
     */
    REJECTED,
    REJETE,

    /**
     * Budget rectifié suite à réallocation ou révision budgétaire.
     */
    RECTIFIE,

    /**
     * Exercice budgétaire clôturé au 31 décembre.
     */
    CLOTURE,
    ARCHIVED;

    /**
     * Indique si le budget est exécutoire et autorise l'engagement de dépenses.
     */
    public boolean isExecutoire() {
        return this == PUBLISHED || this == EXECUTOIRE || this == RECTIFIE;
    }

    /**
     * Indique si le budget est modifiable en saisie de cadrage/tâches.
     */
    public boolean isModifiable() {
        return this == DRAFT || this == EN_PREPARATION || this == REJECTED || this == REJETE;
    }

    /**
     * Indique si l'exercice est définitivement clôturé.
     */
    public boolean isCloture() {
        return this == CLOTURE || this == ARCHIVED;
    }
}
