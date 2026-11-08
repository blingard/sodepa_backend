package com.sodepa.erp.utils;

/**
 * Typologie des tiers et partenaires de la SODEPA (REF-05).
 * <p>
 * Permet de classifier les relations d'affaires, de déterminer le préfixe
 * d'immatriculation du tiers et de déduire automatiquement le compte collectif
 * de rattachement au plan comptable OHADA.
 * </p>
 */
public enum TypeTiers {
    /**
     * Client standard (particulier, restaurant, collectivité) achetant des produits ou prestations.
     */
    CLIENT,

    /**
     * Fournisseur de biens, matériels agricoles, intrants pastoraux ou prestations de services.
     */
    FOURNISSEUR,

    /**
     * Personnel employé de la SODEPA (avances, acomptes, notes de frais).
     */
    PERSONNEL,

    /**
     * Organisme social, fiscal ou institutionnel (CNPS, DGI, Trésor Public, ministères).
     */
    ORGANISME,

    /**
     * Boucherie partenaire, chevillard ou grossiste sous convention commerciale d'abattage et débit.
     */
    BOUCHERIE_GROSSISTE,

    /**
     * Bailleur de fonds ou partenaire technique et financier (Banque Mondiale, BAD, AFD, etc.).
     */
    BAILLEUR_FONDS,

    /**
     * Éleveur ou coopérative pastorale partenaire livrant du bétail pour engraissement ou abattage.
     */
    ELEVEUR_PARTENAIRE
}
