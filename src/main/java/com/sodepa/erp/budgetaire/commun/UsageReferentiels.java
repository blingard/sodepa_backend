package com.sodepa.erp.budgetaire.commun;

import java.util.UUID;

/**
 * Dit si un objet des référentiels budgétaires porte encore du budget.
 *
 * <p>
 * Élément de la nomenclature, structure, nature et source de financement sont
 * les dimensions d'une ligne budgétaire. Un objet cité par une ligne d'un
 * exercice exécutoire et non clos ne se désactive pas : les engagements en
 * cours perdraient leur imputation ou leurs signataires.
 * </p>
 *
 * <p>
 * Une seule interface pour les quatre, parce qu'une seule requête sur les
 * lignes y répond. Les lignes arrivent avec US-BUD-109 (#50), qui fournira
 * l'implémentation réelle ; d'ici là, {@code UsageReferentielsProvisoire}
 * répond « non utilisé ». Les référentiels ne dépendent ainsi pas du module
 * des lignes, qui dépend d'eux.
 * </p>
 */
public interface UsageReferentiels {

    /** Vrai si l'élément ou l'un de ses descendants est cité par une ligne d'un exercice exécutoire et non clos. */
    boolean elementUtilise(UUID elementId);

    /** Vrai si une ligne d'un exercice exécutoire et non clos relève de la structure. */
    boolean structureUtilisee(UUID structureId);

    /** Vrai si une ligne d'un exercice exécutoire et non clos porte cette nature. */
    boolean natureUtilisee(UUID natureId);

    /** Vrai si une ligne d'un exercice exécutoire et non clos est financée par cette source. */
    boolean sourceUtilisee(UUID sourceId);
}
