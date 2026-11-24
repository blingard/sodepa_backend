package com.sodepa.erp.budgetaire.nomenclature.application.outputs;

import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.ElementProgrammatiqueEntity;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.entities.NiveauProgrammatique;

import java.time.Instant;
import java.util.UUID;

/**
 * Fiche d'un élément de la nomenclature.
 *
 * @param id                 identifiant
 * @param code               code du plan de codification
 * @param libelle            libellé
 * @param description        description, éventuellement vide
 * @param niveau             programme, action, activité ou tâche
 * @param parentId           parent, absent pour un programme
 * @param parentCode         code du parent, pour l'affichage
 * @param responsableId      utilisateur responsable
 * @param responsableNom     prénom et nom du responsable
 * @param actif              faux si l'élément a été désactivé
 * @param motifDesactivation motif de la désactivation en cours
 * @param creePar            auteur de la demande de création
 * @param creeLe             date de la demande de création
 * @param validePar          validateur de la création
 * @param valideLe           date de validation de la création
 * @param modifiePar         auteur de la dernière modification validée
 * @param modifieLe          date de validation de la dernière modification
 */
public record ElementProgrammatiqueOutput(
        UUID id,
        String code,
        String libelle,
        String description,
        NiveauProgrammatique niveau,
        UUID parentId,
        String parentCode,
        UUID responsableId,
        String responsableNom,
        boolean actif,
        String motifDesactivation,
        String creePar,
        Instant creeLe,
        String validePar,
        Instant valideLe,
        String modifiePar,
        Instant modifieLe
) {

    public static ElementProgrammatiqueOutput depuis(ElementProgrammatiqueEntity e, String parentCode,
                                                     String responsableNom) {
        return new ElementProgrammatiqueOutput(
                e.getId(), e.getCode(), e.getLibelle(), e.getDescription(), e.getNiveau(), e.getParentId(),
                parentCode, e.getResponsableId(), responsableNom, e.isActif(), e.getMotifDesactivation(),
                e.getCreePar(), e.getCreeLe(), e.getValidePar(), e.getValideLe(), e.getModifiePar(),
                e.getModifieLe());
    }
}
