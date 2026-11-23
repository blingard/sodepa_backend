package com.sodepa.erp.notification.infrastructure.entities;

/**
 * Statuts du cycle de vie d'une entrée dans la table d'outbox transactionnelle des notifications.
 */
public enum OutboxStatus {
    /** Enregistrée dans la transaction métier, en attente de publication RabbitMQ. */
    PENDING,
    /** Publiée dans l'échangeur RabbitMQ, en attente de traitement par le consommateur. */
    PUBLISHED,
    /** Prise en charge en cours par un consommateur asynchrone. */
    PROCESSING,
    /** Expédition terminée (livrée, ignorée ou basculée en repli réussi). */
    COMPLETED,
    /** Échec temporaire planifié pour nouvelle tentative avec backoff exponentiel. */
    FAILED,
    /** Échec définitif après épuisement du nombre maximal de tentatives (routée vers DLQ). */
    DEAD_LETTER
}
