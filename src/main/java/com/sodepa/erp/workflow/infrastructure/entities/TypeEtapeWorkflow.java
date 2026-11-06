package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération qualifiant la nature de l'action attendue lors d'une étape de workflow.
 */
public enum TypeEtapeWorkflow {

    /**
     * Exige une action d'évaluation et de visa humain par un agent désigné (visa hiérarchique, revue technique).
     */
    VALIDATION_HUMAINE,

    /**
     * Étape requérant une signature électronique qualifiée avec certificat ou jeton d'authentification forte
     * (ex: engagements budgétaires supérieurs au seuil critique ou ordonnancement de paiement DG).
     */
    SIGNATURE_ELECTRONIQUE,

    /**
     * Étape exécutée automatiquement par le système sans intervention humaine
     * (ex: contrôle automatisé de disponibilité des crédits budgétaires, vérification de solde bancaire).
     */
    CONTROLE_AUTO,

    /**
     * Étape purement informative déclenchant l'envoi d'alertes ou notifications aux parties prenantes,
     * sans interrompre la progression du circuit.
     */
    NOTIFICATION
}
