package com.sodepa.erp.budgetaire.commun;

import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Une demande en attente sur un référentiel budgétaire, avec son contenu.
 *
 * <p>
 * Reprend les champs de {@code MakerCheckerSmartOutput}, que le front lit déjà,
 * et y ajoute ce dont le valideur a besoin pour décider en connaissance de
 * cause : l'opération réelle, le nom de l'auteur, la fiche proposée et la
 * fiche en vigueur. Sans eux, il approuvait une ligne « UPDATE » sans savoir ce
 * qui changeait, et une création restait invisible, l'entité n'existant pas
 * avant validation.
 * </p>
 *
 * <p>
 * Si la charge d'une demande ne se relit plus (format ancien, objet supprimé),
 * {@code operation}, {@code proposition} et {@code actuel} sont absents : la
 * demande reste listée pour pouvoir être rejetée.
 * </p>
 *
 * @param id                   identifiant de la demande, à passer à {@code validate_or_reject}
 * @param entityName           référentiel visé
 * @param entityPk             identifiant de l'objet visé (connu dès la demande, même pour une création)
 * @param status               toujours {@code PENDING} ici
 * @param checkerOperationType type maker-checker de la demande
 * @param operation            opération demandée : créer, modifier, désactiver ou réactiver
 * @param makerId              identifiant de l'auteur
 * @param auteurNom            prénom et nom de l'auteur, s'il est retrouvé
 * @param createdAt            date de soumission
 * @param expiredAt            au-delà, la demande ne se décide plus
 * @param motif                motif saisi par l'auteur (désactivation, réactivation)
 * @param proposition          fiche telle qu'elle sera après application
 * @param actuel               fiche en vigueur, absente pour une création
 * @param <T>                  type de fiche du référentiel
 */
public record DemandeBudgetaireOutput<T>(
        UUID id,
        MakerCheckerEntityName entityName,
        String entityPk,
        MakerCheckerStatus status,
        MakerCheckerOperationType checkerOperationType,
        OperationReferentiel operation,
        String makerId,
        String auteurNom,
        Instant createdAt,
        Instant expiredAt,
        String motif,
        T proposition,
        T actuel
) {
}
