package com.sodepa.erp.workflow.application;

import java.util.UUID;

/**
 * Événement métier Spring publié lorsqu'une instance de workflow est totalement approuvée
 * ou clôturée au terme de toutes ses étapes avec succès.
 * <p>
 * Ce mécanisme événementiel asynchrone / découplé permet aux modules applicatifs abonnés
 * (ex: {@code RhWorkflowEventListener}, {@code ValiderEtapeWorkflowUseCase} dans le Budget, Achats)
 * de déclencher leurs effets de bord métier respectifs (ex: validation comptable définitive,
 * passage du statut d'engagement à VALIDÉ, génération automatique d'un bon de commande signé).
 * </p>
 *
 * @param instanceId         Identifiant unique de l'instance de workflow terminée.
 * @param workflowCode       Code du circuit de validation ayant abouti (ex: "WF-RH-CONTRAT-TRAVAIL").
 * @param entityName         Nom fonctionnel de l'entité métier ciblée (ex: "CONTRAT_TRAVAIL", "BON_COMMANDE").
 * @param entityId           Identifiant technique de l'entité métier.
 * @param derniereEtapeCode  Code de la dernière étape validée ayant entraîné la clôture.
 * @param dernierActeurId    Identifiant de l'approbateur ayant apposé la dernière signature / décision.
 * @param commentaireFinal   Commentaire éventuel associé à la décision finale.
 */
public record WorkflowInstanceCompletedEvent(
        UUID instanceId,
        String workflowCode,
        String entityName,
        String entityId,
        String derniereEtapeCode,
        UUID dernierActeurId,
        String commentaireFinal
) {}
