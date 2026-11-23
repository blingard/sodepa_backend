package com.sodepa.erp.budgetaire.structure.infrastructure.event;

import com.sodepa.erp.budgetaire.commun.OperationReferentiel;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Charge utile d'une demande portant sur une structure budgétaire, telle que
 * la stocke le maker-checker.
 *
 * <p>
 * Une création ou une modification porte l'ordonnateur et le service
 * d'engagement voulus ; un changement d'état porte son motif. L'unité n'y
 * figure pas : elle se déduit de l'identifiant de la structure, et ne change
 * jamais. La conversion est faite à la main, comme pour les autres
 * référentiels du module : les identifiants reviennent de la base en chaînes.
 * </p>
 *
 * @param operation                opération demandée
 * @param uniteId                  unité déclarée (création seulement)
 * @param ordonnateurPosteId       poste de l'ordonnateur (création, modification)
 * @param serviceEngagementUniteId unité du service d'engagement (création, modification)
 * @param motif                    motif (désactivation, réactivation)
 */
public record DemandeStructureEvent(
        OperationReferentiel operation,
        UUID uniteId,
        UUID ordonnateurPosteId,
        UUID serviceEngagementUniteId,
        String motif
) {

    public static DemandeStructureEvent creation(UUID uniteId, UUID ordonnateurPosteId, UUID serviceEngagementUniteId) {
        return new DemandeStructureEvent(OperationReferentiel.CREER, uniteId, ordonnateurPosteId,
                serviceEngagementUniteId, null);
    }

    public static DemandeStructureEvent modification(UUID ordonnateurPosteId, UUID serviceEngagementUniteId) {
        return new DemandeStructureEvent(OperationReferentiel.MODIFIER, null, ordonnateurPosteId,
                serviceEngagementUniteId, null);
    }

    public static DemandeStructureEvent changementEtat(OperationReferentiel operation, String motif) {
        return new DemandeStructureEvent(operation, null, null, null, motif);
    }

    /** Charge à confier au maker-checker ; les champs vides n'y figurent pas. */
    public Map<String, Object> versCharge() {
        Map<String, Object> charge = new LinkedHashMap<>();
        charge.put("operation", operation.name());
        ajouter(charge, "uniteId", uniteId);
        ajouter(charge, "ordonnateurPosteId", ordonnateurPosteId);
        ajouter(charge, "serviceEngagementUniteId", serviceEngagementUniteId);
        if (motif != null) {
            charge.put("motif", motif);
        }
        return charge;
    }

    public static DemandeStructureEvent depuisCharge(Map<String, Object> charge) {
        if (charge == null) {
            throw illisible();
        }
        try {
            Object motif = charge.get("motif");
            return new DemandeStructureEvent(
                    OperationReferentiel.valueOf(String.valueOf(charge.get("operation"))),
                    uuid(charge, "uniteId"),
                    uuid(charge, "ordonnateurPosteId"),
                    uuid(charge, "serviceEngagementUniteId"),
                    motif == null ? null : motif.toString());
        } catch (IllegalArgumentException e) {
            throw illisible();
        }
    }

    private static void ajouter(Map<String, Object> charge, String cle, UUID valeur) {
        if (valeur != null) {
            charge.put(cle, valeur.toString());
        }
    }

    private static UUID uuid(Map<String, Object> charge, String cle) {
        Object valeur = charge.get(cle);
        return valeur == null ? null : UUID.fromString(valeur.toString());
    }

    private static RequeteInvalideException illisible() {
        return new RequeteInvalideException("DEMANDE_ILLISIBLE",
                "La demande ne décrit pas lisiblement l'opération à appliquer à la structure budgétaire.");
    }
}
