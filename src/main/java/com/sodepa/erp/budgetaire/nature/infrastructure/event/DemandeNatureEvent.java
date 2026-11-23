package com.sodepa.erp.budgetaire.nature.infrastructure.event;

import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Charge utile d'une demande portant sur une nature, telle que la stocke le
 * maker-checker. Seuls les champs utiles à l'opération sont remplis ; une
 * modification ne porte que le libellé.
 *
 * @param operation opération demandée
 * @param code      code normalisé (création)
 * @param libelle   libellé (création, modification)
 * @param section   section (création)
 * @param sens      sens (création)
 * @param compteId  compte du plan comptable, résolu à la demande (création)
 * @param motif     motif (désactivation, réactivation)
 */
public record DemandeNatureEvent(
        OperationReferentiel operation,
        String code,
        String libelle,
        SectionBudgetaire section,
        SensBudgetaire sens,
        UUID compteId,
        String motif
) {

    public static DemandeNatureEvent creation(String code, String libelle, SectionBudgetaire section,
                                              SensBudgetaire sens, UUID compteId) {
        return new DemandeNatureEvent(OperationReferentiel.CREER, code, libelle, section, sens, compteId, null);
    }

    public static DemandeNatureEvent modification(String libelle) {
        return new DemandeNatureEvent(OperationReferentiel.MODIFIER, null, libelle, null, null, null, null);
    }

    public static DemandeNatureEvent changementEtat(OperationReferentiel operation, String motif) {
        return new DemandeNatureEvent(operation, null, null, null, null, null, motif);
    }

    public Map<String, Object> versCharge() {
        Map<String, Object> charge = new LinkedHashMap<>();
        charge.put("operation", operation.name());
        ajouter(charge, "code", code);
        ajouter(charge, "libelle", libelle);
        ajouter(charge, "section", section == null ? null : section.name());
        ajouter(charge, "sens", sens == null ? null : sens.name());
        ajouter(charge, "compteId", compteId == null ? null : compteId.toString());
        ajouter(charge, "motif", motif);
        return charge;
    }

    public static DemandeNatureEvent depuisCharge(Map<String, Object> charge) {
        if (charge == null) {
            throw illisible();
        }
        try {
            String section = texte(charge, "section");
            String sens = texte(charge, "sens");
            String compte = texte(charge, "compteId");
            return new DemandeNatureEvent(
                    OperationReferentiel.valueOf(texte(charge, "operation")),
                    texte(charge, "code"),
                    texte(charge, "libelle"),
                    section == null ? null : SectionBudgetaire.valueOf(section),
                    sens == null ? null : SensBudgetaire.valueOf(sens),
                    compte == null ? null : UUID.fromString(compte),
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
                "La demande ne décrit pas lisiblement l'opération à appliquer à la nature.");
    }
}
