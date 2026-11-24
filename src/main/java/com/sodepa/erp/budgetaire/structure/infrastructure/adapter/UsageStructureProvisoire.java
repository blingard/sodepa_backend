package com.sodepa.erp.budgetaire.structure.infrastructure.adapter;

import com.sodepa.erp.budgetaire.structure.application.UsageStructure;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Réponse provisoire tant que les lignes budgétaires n'existent pas : sans
 * ligne, aucune structure n'est utilisée. US-BUD-109 (#50) remplacera cette
 * classe par la requête sur les lignes des exercices exécutoires non clos.
 */
@Component
public class UsageStructureProvisoire implements UsageStructure {

    @Override
    public boolean estUtilisee(UUID structureId) {
        return false;
    }
}
