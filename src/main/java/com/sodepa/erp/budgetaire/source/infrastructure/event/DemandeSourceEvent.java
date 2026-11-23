package com.sodepa.erp.budgetaire.source.infrastructure.event;

import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Charge utile d'une demande portant sur une source de financement. Une
 * modification porte l'état voulu du libellé et du bailleur : un bailleur
 * absent est un bailleur effacé.
 *
 * @param operation opération demandée
 * @param code      code normalisé (création)
 * @param libelle   libellé (création, modification)
 * @param type      type de source (création)
 * @param bailleur  partenaire ou bailleur (création, modification)
 * @param motif     motif (désactivation, réactivation)
 */
public record DemandeSourceEvent(
        OperationReferentiel operation,
        String code,
        String libelle,
        TypeSourceFinancement type,
        String bailleur,
        String motif
) {

    public static DemandeSourceEvent creation(String code, String libelle, TypeSourceFinancement type, String bailleur) {
        return new DemandeSourceEvent(OperationReferentiel.CREER, code, libelle, type, bailleur, null);
    }

    public static DemandeSourceEvent modification(String libelle, String bailleur) {
        return new DemandeSourceEvent(OperationReferentiel.MODIFIER, null, libelle, null, bailleur, null);
    }

    public static DemandeSourceEvent changementEtat(OperationReferentiel operation, String motif) {
        return new DemandeSourceEvent(operation, null, null, null, null, motif);
    }

    public Map<String, Object> versCharge() {
        Map<String, Object> charge = new LinkedHashMap<>();
        charge.put("operation", operation.name());
        ajouter(charge, "code", code);
        ajouter(charge, "libelle", libelle);
        ajouter(charge, "type", type == null ? null : type.name());
        ajouter(charge, "bailleur", bailleur);
        ajouter(charge, "motif", motif);
        return charge;
    }

    public static DemandeSourceEvent depuisCharge(Map<String, Object> charge) {
        if (charge == null) {
            throw illisible();
        }
        try {
            String type = texte(charge, "type");
            return new DemandeSourceEvent(
                    OperationReferentiel.valueOf(texte(charge, "operation")),
                    texte(charge, "code"),
                    texte(charge, "libelle"),
                    type == null ? null : TypeSourceFinancement.valueOf(type),
                    texte(charge, "bailleur"),
                    texte(charge, "motif"));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw illisible();
        }
    }

    private static void ajouter(Map<String, Object> charge, String cle, String valeur) {
        if (valeur != null) {
            charge.put(cle, valeur);
        }
    }

    private static String texte(Map<String, Object> charge, String cle) {
        Object valeur = charge.get(cle);
        return valeur == null ? null : valeur.toString();
    }

    private static RequeteInvalideException illisible() {
        return new RequeteInvalideException("DEMANDE_ILLISIBLE",
                "La demande ne décrit pas lisiblement l'opération à appliquer à la source de financement.");
    }
}
