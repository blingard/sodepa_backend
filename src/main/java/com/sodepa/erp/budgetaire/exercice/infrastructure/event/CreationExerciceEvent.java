package com.sodepa.erp.budgetaire.exercice.infrastructure.event;

import com.sodepa.erp.share.erreurs.RequeteInvalideException;

import java.util.Map;

/**
 * Charge utile d'une demande de création d'exercice, telle que la stocke le
 * maker-checker.
 *
 * <p>
 * Elle ne porte que l'année : dates et état se déduisent à la validation, et
 * l'auteur est celui de la demande. La conversion est faite à la main plutôt
 * que par un {@code ObjectMapper}, parce que la charge revient de la base en
 * {@code Map} JSON où un entier peut ressortir en {@code Long} ou en
 * {@code Integer} selon le pilote.
 * </p>
 *
 * @param annee année de l'exercice demandé
 */
public record CreationExerciceEvent(int annee) {

    private static final String CLE_ANNEE = "annee";

    public Map<String, Object> versCharge() {
        return Map.of(CLE_ANNEE, annee);
    }

    public static CreationExerciceEvent depuisCharge(Map<String, Object> charge) {
        Object valeur = charge == null ? null : charge.get(CLE_ANNEE);
        if (valeur instanceof Number nombre) {
            return new CreationExerciceEvent(nombre.intValue());
        }
        throw new RequeteInvalideException("DEMANDE_ILLISIBLE",
                "La demande ne précise pas l'année de l'exercice à créer.");
    }
}
