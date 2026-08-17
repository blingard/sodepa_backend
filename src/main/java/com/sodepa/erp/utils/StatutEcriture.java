package com.sodepa.erp.utils;

/**
 * Énumération représentant le statut d'une écriture comptable au sein du workflow de validation (SYSCOHADA).
 */
public enum StatutEcriture {
    /**
     * Écriture en état de projet ou de brouillon, modifiable par son créateur, exclue du grand livre et des rapports officiels.
     */
    BROUILLON,
    
    /**
     * Écriture soumise à la validation, en attente de vérification par un chef comptable ou un administrateur.
     */
    SOUMIS,
    
    /**
     * Écriture validée et verrouillée de manière intangible. Seules les écritures validées impactent le grand livre et les liasses fiscales.
     */
    VALIDE,
    
    /**
     * Écriture rejetée après contrôle, nécessitant des corrections par le comptable.
     */
    REJETE
}
