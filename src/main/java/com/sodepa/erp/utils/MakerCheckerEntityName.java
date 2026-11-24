package com.sodepa.erp.utils;

/**
 * Composant applicatif `MakerCheckerEntityName` du système ERP SODEPA.
 */
public enum MakerCheckerEntityName {
    BANQUE, FISCALYEARCLOSING, TIERS, COMPTE, ECRITURE, IMMOBILISATION, JOURNAL, LIGNEECRITURE, LIGNERELEVEBANCAIRE, RELEVEBANCAIRE, USER,
    MARCHE_RECEPTION_PV, LIQUIDATION_FACTURE, MARCHE_DOSSIER, FOURNISSEUR, EXPRESSION_BESOIN,
    EMPLOYE, DEROGATION_PERMISSION,
    UNITE_ORGANISATIONNELLE, POSTE, AFFECTATION_POSTE, DELEGATION_SIGNATURE,
    REGIE_APPROVISIONNEMENT, REGIE_DEPENSE,
    BOVIN_NAISSANCE, BOVIN_MOUVEMENT, BOVIN_SORTIE, HERD_COMPOSITION,
    PAIE_PERIODE, PAIE_VARIABLE,
    /*
     * Types de l'ancien paquet budget/, supprimé. Plus aucun code ne les soumet ni ne les
     * valide ; ils restent déclarés parce que maker_checker_requests.entity_name est lu en
     * @Enumerated(STRING) : une demande déjà enregistrée avec ces valeurs ne se chargerait plus.
     * À retirer une fois ces lignes purgées.
     */
    BUDGET_PLAN, BUDGET_ENGAGEMENT, BUDGET_REALLOCATION
}
