package com.sodepa.erp.paie.infrastructure.entities;

/**
 * Entité de persistance représentant `TypeRubrique` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum TypeRubrique {
    SALAIRE_BASE,
    SUR_SALAIRE,
    PRIME_LOGEMENT,
    INDEMNITE_TRANSPORT,
    PRIME_RISQUE,
    PRIME_FONCTION,
    HEURES_SUPPLEMENTAIRES,
    COTISATION_SOCIALE,
    IMPOT_TAXE,
    ACOMPTE,
    AUTRE_RETENUE
}
