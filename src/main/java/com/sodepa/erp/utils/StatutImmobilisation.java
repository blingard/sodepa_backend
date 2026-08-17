package com.sodepa.erp.utils;

/**
 * Énumération des statuts d'une immobilisation dans le registre des actifs.
 * Permet de suivre le cycle de vie de l'actif (acquisition, détention, sortie).
 */
public enum StatutImmobilisation {
    /** Actif en service, sujet à amortissement si applicable. */
    ACTIVE,

    /** Actif vendu ou transféré. */
    CEDEE,

    /** Actif retiré du service et détruit ou mis hors d'usage. */
    MISE_AU_REBUT
}
