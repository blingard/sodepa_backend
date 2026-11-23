package com.sodepa.erp.budgetaire.nomenclature.application;

import java.util.UUID;

/**
 * Dit si un élément de la nomenclature porte encore du budget.
 *
 * <p>
 * Un élément cité par une ligne budgétaire d'un exercice exécutoire et non
 * clos ne se désactive pas (US-BUD-104, scénario « désactivation d'un élément
 * utilisé ») : les engagements en cours perdraient leur imputation.
 * </p>
 *
 * <p>
 * Les lignes budgétaires n'existent pas encore : elles arrivent avec
 * US-BUD-109 (#50), qui fournira l'implémentation réelle. D'ici là,
 * {@code UsageNomenclatureProvisoire} répond qu'aucun élément n'est utilisé.
 * L'interface sépare la nomenclature des lignes : la nomenclature ne dépend
 * pas du module qui la consomme.
 * </p>
 */
public interface UsageNomenclature {

    /**
     * @param elementId élément de la nomenclature, à n'importe quel niveau
     * @return vrai si l'élément ou l'un de ses descendants est cité par une
     *         ligne d'un exercice exécutoire et non clos
     */
    boolean estUtilise(UUID elementId);
}
