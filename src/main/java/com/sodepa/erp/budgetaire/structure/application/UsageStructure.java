package com.sodepa.erp.budgetaire.structure.application;

import java.util.UUID;

/**
 * Dit si une structure budgétaire porte encore du budget.
 *
 * <p>
 * Une structure citée par une ligne d'un exercice exécutoire et non clos ne
 * se désactive pas : ses engagements en cours perdraient leurs signataires.
 * Les lignes arrivent avec US-BUD-109 (#50), qui fournira l'implémentation
 * réelle ; d'ici là, {@code UsageStructureProvisoire} répond « non utilisée »,
 * comme pour la nomenclature.
 * </p>
 */
public interface UsageStructure {

    /**
     * @param structureId structure budgétaire
     * @return vrai si une ligne d'un exercice exécutoire et non clos la cite
     */
    boolean estUtilisee(UUID structureId);
}
