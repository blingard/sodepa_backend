package com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter;

import com.sodepa.erp.budgetaire.nomenclature.application.UsageNomenclature;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Réponse provisoire tant que les lignes budgétaires n'existent pas.
 *
 * <p>
 * Sans ligne budgétaire, aucun élément ne peut être utilisé : la réponse
 * « non » est exacte aujourd'hui. US-BUD-109 (#50) remplacera cette classe par
 * la requête sur les lignes des exercices exécutoires non clos, et y rejouera
 * le scénario « désactivation d'un élément utilisé ».
 * </p>
 */
@Component
public class UsageNomenclatureProvisoire implements UsageNomenclature {

    @Override
    public boolean estUtilise(UUID elementId) {
        return false;
    }
}
