package com.sodepa.erp.achats.infrastructure.entities;

/**
 * Entité de persistance représentant `TypeProcedureAchat` au sein du système ERP SODEPA.
 * Assure le stockage relationnel et la traçabilité des données du domaine.
 */
public enum TypeProcedureAchat {
    ACHAT_DIRECT("AD"),
    DEMANDE_COTATION("DC"),
    BON_COMMANDE("BC"),
    LETTRE_COMMANDE("LC"),
    MARCHE_PUBLIC_CIPM("MP");
    /**
     * Champ prefix : prefix utilisé pour le pilotage et le suivi dans TypeProcedureAchat.
     */
    private final String prefix;

    private TypeProcedureAchat(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
