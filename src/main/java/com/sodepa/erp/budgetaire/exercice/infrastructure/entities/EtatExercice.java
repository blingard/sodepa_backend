package com.sodepa.erp.budgetaire.exercice.infrastructure.entities;

/**
 * Cycle de vie d'un exercice budgétaire (spécification § 5).
 *
 * <p>
 * L'ordre est imposé par la procédure : le budget est préparé, approuvé par le
 * Conseil d'Administration, rendu exécutoire par le DG, puis clos. Seul un
 * exercice {@link #EXECUTOIRE} accepte visas et engagements (RG-BUD-002).
 * Les transitions arrivent avec US-BUD-102, 103 et 407.
 * </p>
 */
public enum EtatExercice {
    /** Créé, en attente du chargement et de l'approbation de son budget. */
    EN_PREPARATION,
    /** Budget approuvé par résolution du Conseil d'Administration. */
    APPROUVE_CA,
    /** Décision exécutoire signée : les crédits sont ouverts. */
    EXECUTOIRE,
    /** Exercice arrêté : plus aucune opération. */
    CLOS
}
