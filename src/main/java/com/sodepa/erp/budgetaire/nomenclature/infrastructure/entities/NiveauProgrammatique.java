package com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities;

/**
 * Niveau d'un élément de la nomenclature budgétaire, du plus large au plus fin.
 *
 * <p>
 * La SODEPA budgétise en programme → action → activité → tâche (procédure
 * budgétaire, budget-programme). Chaque niveau se rattache au niveau juste
 * au-dessus : la règle est écrite une seule fois, ici, pour les quatre.
 * </p>
 *
 * <p>
 * US-BUD-104 ne gère que les programmes et les actions ; US-BUD-105 ouvrira
 * les activités et les tâches (voir {@link #gereDepuis104()}).
 * </p>
 */
public enum NiveauProgrammatique {

    PROGRAMME,
    ACTION,
    ACTIVITE,
    TACHE;

    /**
     * Niveau que doit avoir le parent d'un élément de ce niveau.
     *
     * @return {@code null} pour un programme, qui n'a pas de parent
     */
    public NiveauProgrammatique niveauParent() {
        return ordinal() == 0 ? null : values()[ordinal() - 1];
    }

    /**
     * Niveaux saisissables aujourd'hui. Les activités et les tâches attendent
     * leurs indicateurs et leurs cibles (US-BUD-105) : les créer sans eux
     * donnerait des éléments incomplets à reprendre.
     */
    public boolean gereDepuis104() {
        return this == PROGRAMME || this == ACTION;
    }

    /** Libellé lisible, pour les messages d'erreur adressés à l'utilisateur. */
    public String libelle() {
        return switch (this) {
            case PROGRAMME -> "programme";
            case ACTION -> "action";
            case ACTIVITE -> "activité";
            case TACHE -> "tâche";
        };
    }

    /** Libellé précédé de l'article indéfini : « un programme », « une action ». */
    public String avecArticle() {
        return (this == PROGRAMME ? "un " : "une ") + libelle();
    }
}
