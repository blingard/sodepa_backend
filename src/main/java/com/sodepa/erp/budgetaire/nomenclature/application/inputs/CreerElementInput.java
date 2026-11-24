package com.sodepa.erp.budgetaire.nomenclature.application.inputs;

import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;

import java.util.UUID;

/**
 * Demande de création d'un élément de la nomenclature, telle que reçue.
 *
 * <p>Les valeurs ne sont pas encore normalisées : le code est mis en
 * majuscules et contrôlé par l'adaptateur.</p>
 *
 * @param niveau        programme ou action (activités et tâches : US-BUD-105)
 * @param code          code du plan de codification
 * @param libelle       libellé
 * @param description   description facultative
 * @param parentId      élément du niveau juste au-dessus ; absent pour un programme
 * @param responsableId utilisateur responsable ; obligatoire pour un programme
 */
public record CreerElementInput(
        NiveauProgrammatique niveau,
        String code,
        String libelle,
        String description,
        UUID parentId,
        UUID responsableId
) {
}
