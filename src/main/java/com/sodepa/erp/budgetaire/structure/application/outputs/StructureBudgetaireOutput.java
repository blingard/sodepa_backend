package com.sodepa.erp.budgetaire.structure.application.outputs;

import java.time.Instant;
import java.util.UUID;

/**
 * Structure budgétaire telle que la lit le client, avec ce qu'il faut de
 * l'organigramme pour l'afficher sans second appel.
 *
 * @param id                         identifiant de la structure
 * @param uniteId                    unité déclarée
 * @param uniteCode                  code de l'unité
 * @param uniteIntitule              libellé de l'unité
 * @param uniteActive                faux si l'unité a été fermée dans l'organigramme depuis la déclaration
 * @param ordonnateurPosteId         poste de l'ordonnateur
 * @param ordonnateurPosteIntitule   libellé du poste (« Directeur Général »)
 * @param ordonnateurTitulaireId     titulaire actuel du poste ; absent si le poste est vacant
 * @param ordonnateurTitulaireNom    prénom et nom du titulaire
 * @param serviceEngagementUniteId   unité du service d'engagement
 * @param serviceEngagementCode      code de cette unité
 * @param serviceEngagementIntitule  libellé de cette unité (« Service du Budget »)
 * @param actif                      faux si la structure a été désactivée
 * @param motifDesactivation         motif de la désactivation en cours
 * @param creePar                    auteur de la demande de déclaration
 * @param creeLe                     date de la demande
 * @param validePar                  validateur de la déclaration
 * @param valideLe                   date de validation
 * @param modifiePar                 auteur de la dernière modification validée
 * @param modifieLe                  date de validation de la dernière modification
 */
public record StructureBudgetaireOutput(
        UUID id,
        UUID uniteId,
        String uniteCode,
        String uniteIntitule,
        boolean uniteActive,
        UUID ordonnateurPosteId,
        String ordonnateurPosteIntitule,
        UUID ordonnateurTitulaireId,
        String ordonnateurTitulaireNom,
        UUID serviceEngagementUniteId,
        String serviceEngagementCode,
        String serviceEngagementIntitule,
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
