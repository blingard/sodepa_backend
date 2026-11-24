package com.sodepa.erp.budgetaire.nature.application.outputs;

import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SectionBudgetaire;
import com.sodepa.erp.budgetaire.nature.infrastructure.entities.SensBudgetaire;

import java.time.Instant;
import java.util.UUID;

/**
 * Nature budgétaire telle que la lit le client.
 *
 * @param id                 identifiant
 * @param code               code de la nature
 * @param libelle            libellé
 * @param section            fonctionnement, investissement ou produits
 * @param sens               dépense ou recette
 * @param compteId           compte du plan comptable
 * @param compteCode         code du compte (« 701 »)
 * @param compteIntitule     intitulé du compte
 * @param actif              faux si la nature a été désactivée
 * @param motifDesactivation motif de la désactivation en cours
 * @param creePar            auteur de la demande de création
 * @param creeLe             date de la demande
 * @param validePar          validateur de la création
 * @param valideLe           date de validation
 * @param modifiePar         auteur de la dernière modification validée
 * @param modifieLe          date de validation de la dernière modification
 */
public record NatureBudgetaireOutput(
        UUID id,
        String code,
        String libelle,
        SectionBudgetaire section,
        SensBudgetaire sens,
        UUID compteId,
        String compteCode,
        String compteIntitule,
        boolean actif,
        String motifDesactivation,
        String creePar,
        Instant creeLe,
        String validePar,
        Instant valideLe,
        String modifiePar,
        Instant modifieLe
) {
}
