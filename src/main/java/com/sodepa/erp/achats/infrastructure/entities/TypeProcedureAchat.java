package com.sodepa.erp.achats.infrastructure.entities;

public enum TypeProcedureAchat {
    ACHAT_DIRECT("AD"),
    DEMANDE_COTATION("DC"),
    BON_COMMANDE("BC"),
    LETTRE_COMMANDE("LC"),
    MARCHE_PUBLIC_CIPM("MP");
    private final String prefix;

    private TypeProcedureAchat(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
