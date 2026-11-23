package com.sodepa.erp.budgetaire.nomenclature.infrastructure.event;

import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;
import com.sodepa.erp.share.erreurs.RequeteInvalideException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Charge utile d'une demande portant sur un élément de la nomenclature, telle
 * que la stocke le maker-checker.
 *
 * <p>
 * Une seule forme pour les quatre opérations : seuls les champs utiles à
 * l'opération sont remplis. Pour une modification, la charge porte l'état
 * voulu complet des champs modifiables (libellé, description, responsable) :
 * un champ absent est un champ vidé, pas un champ inchangé.
 * </p>
 *
 * <p>
 * La conversion est faite à la main, comme pour l'exercice : la charge revient
 * de la base en {@code Map} JSON, et les identifiants y sont des chaînes.
 * </p>
 *
 * @param operation     opération demandée
 * @param niveau        niveau de l'élément (création)
 * @param code          code normalisé en majuscules (création)
 * @param libelle       libellé (création, modification)
 * @param description   description facultative (création, modification)
 * @param parentId      parent (création d'un élément qui n'est pas un programme)
 * @param responsableId utilisateur responsable (création, modification)
 * @param motif         motif (désactivation, réactivation)
 */
public record DemandeNomenclatureEvent(
        OperationNomenclature operation,
        NiveauProgrammatique niveau,
        String code,
        String libelle,
        String description,
        UUID parentId,
        UUID responsableId,
        String motif
) {

    public static DemandeNomenclatureEvent creation(NiveauProgrammatique niveau, String code, String libelle,
                                                    String description, UUID parentId, UUID responsableId) {
        return new DemandeNomenclatureEvent(OperationNomenclature.CREER, niveau, code, libelle, description,
                parentId, responsableId, null);
    }

    public static DemandeNomenclatureEvent modification(String libelle, String description, UUID responsableId) {
        return new DemandeNomenclatureEvent(OperationNomenclature.MODIFIER, null, null, libelle, description,
                null, responsableId, null);
    }

    public static DemandeNomenclatureEvent changementEtat(OperationNomenclature operation, String motif) {
        return new DemandeNomenclatureEvent(operation, null, null, null, null, null, null, motif);
    }

    /** Charge à confier au maker-checker ; les champs vides n'y figurent pas. */
    public Map<String, Object> versCharge() {
        Map<String, Object> charge = new LinkedHashMap<>();
        charge.put("operation", operation.name());
        ajouter(charge, "niveau", niveau == null ? null : niveau.name());
        ajouter(charge, "code", code);
        ajouter(charge, "libelle", libelle);
        ajouter(charge, "description", description);
        ajouter(charge, "parentId", parentId == null ? null : parentId.toString());
        ajouter(charge, "responsableId", responsableId == null ? null : responsableId.toString());
        ajouter(charge, "motif", motif);
        return charge;
    }

    public static DemandeNomenclatureEvent depuisCharge(Map<String, Object> charge) {
        if (charge == null) {
            throw illisible();
        }
        try {
            String niveau = texte(charge, "niveau");
            String parent = texte(charge, "parentId");
            String responsable = texte(charge, "responsableId");
            return new DemandeNomenclatureEvent(
                    OperationNomenclature.valueOf(texte(charge, "operation")),
                    niveau == null ? null : NiveauProgrammatique.valueOf(niveau),
                    texte(charge, "code"),
                    texte(charge, "libelle"),
                    texte(charge, "description"),
                    parent == null ? null : UUID.fromString(parent),
                    responsable == null ? null : UUID.fromString(responsable),
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
                "La demande ne décrit pas lisiblement l'opération à appliquer à la nomenclature.");
    }
}
