package com.sodepa.erp.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Énumération des codes de journaux comptables autorisés avec leur code court et description associée.
 * Assure la cohérence des codes utilisés pour la ventilation des écritures.
 */
@Getter
@RequiredArgsConstructor
public enum CodeJournal {
    /** Journal des Achats. */
    HA("HA", "Journal des Achats"),

    /** Journal des Ventes. */
    VT("VT", "Journal des Ventes"),

    /** Journal de Banque. */
    BQ("BQ", "Journal de Banque"),

    /** Journal de Caisse. */
    CA("CA", "Journal de Caisse"),

    /** Journal des Opérations Diverses. */
    OD("OD", "Journal des Opérations Diverses"),

    /** Journal de Report à Nouveau (ouverture). */
    RAN("RAN", "Report à Nouveau");

    private final String code;
    private final String description;
}
