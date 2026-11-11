package com.sodepa.erp.budget.infrastructure.entities;

/**
 * Cycle de vie des réallocations de crédits budgétaires conformément
 * à la procédure officielle SODEPA (Section 9 du CdC et F-BUD-021 à F-BUD-024).
 * 
 * Brouillon → Soumise → Analysée → Avis comité → Décision DG → (CA) → Exécutée / Rejetée / Complément demandé
 * Mesure conservatoire d'extrême urgence : statut « À régulariser CA ».
 */
public enum StatutReallocation {
    /**
     * Demande initiée par le responsable de l'UO / chef de programme.
     */
    BROUILLON,

    /**
     * Demande soumise pour instruction financière.
     */
    SOUMISE,

    /**
     * Demande analysée par le Service du Budget (CSB / SDFBM).
     */
    ANALYSEE,

    /**
     * Avis formulé par le Comité d'analyse / commission budgétaire (DAF, SDFBM, DCAI).
     */
    AVIS_COMITE,

    /**
     * Arbitrée et approuvée par le Directeur Général.
     */
    DECISION_DG,

    /**
     * Approuvée par résolution du Conseil d'Administration (pour réallocations significatives).
     */
    APPROUVEE_CA,

    /**
     * Mesure conservatoire d'extrême urgence autorisée par le DG (RG-BUD-017 / CA-BUD-023-03),
     * permettant l'exécution immédiate, avec régularisation impérative à la session suivante du CA.
     */
    A_REGULARISER_CA,

    /**
     * Réallocation appliquée en comptabilité budgétaire (dotations source et cible rectifiées).
     */
    EXECUTEE,

    /**
     * Demande rejetée avec motif explicite.
     */
    REJETEE,

    /**
     * Complément d'information ou justification requis.
     */
    COMPLEMENT_DEMANDE;

    /**
     * Indique si la réallocation permet le déblocage et l'engagement effectif des crédits sur la ligne cible.
     */
    public boolean autoriseEngagementCible() {
        return this == EXECUTEE || this == A_REGULARISER_CA;
    }
}
