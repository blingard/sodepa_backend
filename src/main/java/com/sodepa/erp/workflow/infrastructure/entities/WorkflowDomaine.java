package com.sodepa.erp.workflow.infrastructure.entities;

/**
 * Énumération des grands domaines fonctionnels et unités opérationnelles gérés
 * par le moteur de workflow universel de la SODEPA.
 * <p>
 * Permet de classifier et cloisonner les circuits de validation selon les pôles métier.
 * </p>
 */
public enum WorkflowDomaine {

    /**
     * Gouvernance institutionnelle, Direction Générale, Conseil d'Administration et Décisions Stratégiques.
     */
    GOUVERNANCE,

    /**
     * Gestion et exécution budgétaire (engagements, liquidations, ordonnancements, virements de crédits).
     */
    BUDGET,

    /**
     * Comptabilité générale, analytique, opérations d'inventaire, clôtures mensuelles et lettrages.
     */
    COMPTABILITE,

    /**
     * Gestion Électronique des Documents, conventions, contrats commerciaux, visas juridiques.
     */
    GED,

    /**
     * Trésorerie, décaissements, ordres de virement, gestion des caisses locales et rapprochements bancaires.
     */
    FINANCE,

    /**
     * Ressources Humaines, paie, congés, ordres de mission sur le terrain, recrutements, sanctions.
     */
    RH,

    /**
     * Achats publics et locaux, bons de commande, réceptions, gestion des fournisseurs.
     */
    ACHATS,

    /**
     * Gestion générale de la production animale, rendements, suivi vétérinaire et zootechnique.
     */
    PRODUCTION,

    /**
     * Vente de bétail sur pied, viande foraine, distribution en boucheries et facturation clients.
     */
    COMMERCIAL,

    /**
     * Activités et logistique pastorale propres aux ranchs d'élevage (Jakiri, Dumbo, Faro).
     */
    PASTORAL,

    /**
     * Exploitation des abattoirs industriels frigorifiques (Yaoundé, Douala, Ngaoundéré).
     */
    ABATTOIR,

    /**
     * Production laitière, transformation et distribution de produits laitiers (Station de Jakiri).
     */
    LAITERIE,

    /**
     * Contrôle qualité, audit interne, conformité sanitaire QHSE et prospective.
     */
    AUDIT_QHSE,
}
