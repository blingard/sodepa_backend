package com.sodepa.erp.utils;

/**
 * Énumération des devises supportées par le système comptable (norme ISO 4217).
 * Utilisée pour assurer la cohérence des devises enregistrées dans les pièces du grand livre.
 */
public enum Devise {
    /** Franc CFA (UEMOA) - Devise principale de la zone Ouest Africaine. */
    XOF,

    /** Franc CFA (CEMAC) - Devise de la zone Afrique Centrale. */
    XAF,

    /** Euro - Devise européenne. */
    EUR,

    /** Dollar Américain. */
    USD
}
