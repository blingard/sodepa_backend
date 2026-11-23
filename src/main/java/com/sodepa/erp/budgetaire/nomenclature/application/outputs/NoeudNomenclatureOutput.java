package com.sodepa.erp.budgetaire.nomenclature.application.outputs;

import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;

import java.util.List;
import java.util.UUID;

/**
 * Nœud de l'arbre de la nomenclature, avec ses enfants triés par code.
 *
 * <p>Allégé par rapport à la fiche : l'arbre sert à naviguer, la fiche
 * ({@code GET /api/budget/nomenclature/{id}}) donne le détail et l'historique.</p>
 *
 * @param id             identifiant
 * @param code           code du plan de codification
 * @param libelle        libellé
 * @param niveau         niveau de l'élément
 * @param responsableId  utilisateur responsable
 * @param responsableNom prénom et nom du responsable
 * @param actif          faux si l'élément a été désactivé
 * @param enfants        éléments du niveau juste en dessous
 */
public record NoeudNomenclatureOutput(
        UUID id,
        String code,
        String libelle,
        NiveauProgrammatique niveau,
        UUID responsableId,
        String responsableNom,
        boolean actif,
        List<NoeudNomenclatureOutput> enfants
) {
}
