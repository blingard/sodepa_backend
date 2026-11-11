package com.sodepa.erp.budget.infrastructure.entities;

/**
 * Classification des réallocations budgétaires SODEPA selon les règles RG-BUD-015, RG-BUD-016 et RG-BUD-018.
 */
public enum TypeReallocation {
    /**
     * Réallocation simple : montant ≤ 5% de la dotation initiale de la ligne cédante,
     * au sein de la même unité opérationnelle et pour des lignes de même nature (fonctionnement).
     * Circuit décisionnel : DAF + Directeur Général.
     */
    SIMPLE,

    /**
     * Réallocation significative : montant > 5% de la dotation initiale,
     * ou impliquant un changement d'unité / de programme, ou portant sur des dépenses d'investissement.
     * Circuit décisionnel : Commission budgétaire + DG + Résolution du Conseil d'Administration.
     */
    SIGNIFICATIVE
}
