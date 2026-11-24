package com.sodepa.erp.budgetaire.commun;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Réponse provisoire tant que les lignes budgétaires n'existent pas : sans
 * ligne, aucun objet n'est utilisé, et la réponse « non » est exacte
 * aujourd'hui. US-BUD-109 (#50) remplacera cette classe par la requête sur les
 * lignes des exercices exécutoires non clos, et y rejouera les refus de
 * désactivation.
 */
@Component
public class UsageReferentielsProvisoire implements UsageReferentiels {

    @Override
    public boolean elementUtilise(UUID elementId) {
        return false;
    }

    @Override
    public boolean structureUtilisee(UUID structureId) {
        return false;
    }

    @Override
    public boolean natureUtilisee(UUID natureId) {
        return false;
    }

    @Override
    public boolean sourceUtilisee(UUID sourceId) {
        return false;
    }
}
