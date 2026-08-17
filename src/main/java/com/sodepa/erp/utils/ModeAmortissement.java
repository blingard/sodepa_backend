package com.sodepa.erp.utils;

/**
 * Énumération des modes d'amortissement appliqués aux actifs immobilisés.
 * Conforme aux règles comptables et fiscales de l'OHADA.
 */
public enum ModeAmortissement {
    /** Amortissement linéaire. */
    LINEAIRE,

    /** Amortissement dégressif. */
    DEGRESSIF,

    /** Amortissement accéléré. */
    ACCELERE
}
