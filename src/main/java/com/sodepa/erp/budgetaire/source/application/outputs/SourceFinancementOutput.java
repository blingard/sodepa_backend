package com.sodepa.erp.budgetaire.source.application.outputs;

import com.sodepa.erp.budgetaire.source.infrastructure.entities.SourceFinancementEntity;
import com.sodepa.erp.budgetaire.source.infrastructure.entities.TypeSourceFinancement;

import java.time.Instant;
import java.util.UUID;

/**
 * Source de financement telle que la lit le client.
 *
 * @param id                 identifiant
 * @param code               code
 * @param libelle            libellé
 * @param type               ressources propres, subvention ou partenaire
 * @param bailleur           partenaire ou bailleur
 * @param actif              faux si la source a été désactivée
 * @param motifDesactivation motif de la désactivation en cours
 * @param creePar            auteur de la demande de création
 * @param creeLe             date de la demande
 * @param validePar          validateur de la création
 * @param valideLe           date de validation
 * @param modifiePar         auteur de la dernière modification validée
 * @param modifieLe          date de validation de la dernière modification
 */
public record SourceFinancementOutput(
        UUID id,
        String code,
        String libelle,
        TypeSourceFinancement type,
        String bailleur,
        boolean actif,
        String motifDesactivation,
        String creePar,
        Instant creeLe,
        String validePar,
        Instant valideLe,
        String modifiePar,
        Instant modifieLe
) {

    public static SourceFinancementOutput depuis(SourceFinancementEntity s) {
        return new SourceFinancementOutput(s.getId(), s.getCode(), s.getLibelle(), s.getType(), s.getBailleur(),
                s.isActif(), s.getMotifDesactivation(), s.getCreePar(), s.getCreeLe(), s.getValidePar(),
                s.getValideLe(), s.getModifiePar(), s.getModifieLe());
    }
}
