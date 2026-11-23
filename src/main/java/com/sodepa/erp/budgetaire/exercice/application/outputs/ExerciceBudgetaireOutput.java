package com.sodepa.erp.budgetaire.exercice.application.outputs;

import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.EtatExercice;
import com.sodepa.erp.budgetaire.exercice.infrastructure.entities.ExerciceBudgetaireEntity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Exercice budgétaire tel que le lit le client.
 *
 * @param id         identifiant de l'exercice
 * @param annee      année civile
 * @param etat       état dans le cycle préparation → approbation → exécution → clôture
 * @param dateDebut  1er janvier
 * @param dateFin    31 décembre
 * @param creePar    auteur de la demande de création
 * @param creeLe     date de la demande
 * @param validePar  validateur de la demande
 * @param valideLe   date de la validation
 */
public record ExerciceBudgetaireOutput(
        UUID id,
        int annee,
        EtatExercice etat,
        LocalDate dateDebut,
        LocalDate dateFin,
        String creePar,
        Instant creeLe,
        String validePar,
        Instant valideLe
) {

    public static ExerciceBudgetaireOutput depuis(ExerciceBudgetaireEntity e) {
        return new ExerciceBudgetaireOutput(
                e.getId(), e.getAnnee(), e.getEtat(), e.getDateDebut(), e.getDateFin(),
                e.getCreePar(), e.getCreeLe(), e.getValidePar(), e.getValideLe());
    }
}
