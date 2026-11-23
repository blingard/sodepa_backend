package com.sodepa.erp.notification.infrastructure.entities;

/**
 * États du cycle de vie d'une notification par canal effectif ERP SODEPA.
 */
public enum StatutNotification {
    /** Notification enregistrée et en file d'attente d'expédition asynchrone. */
    PENDING,
    /** Notification transmise avec succès à la passerelle cible (SMTP, SMS, WhatsApp) ou prête en cloche In-App. */
    SENT,
    /** Alias historique normalisé en {@link #SENT} en base de données. */
    @Deprecated(forRemoval = false)
    SEND,
    /** Échec d'acheminement technique (trace d'erreur conservée pour rejeu/DLQ). */
    FAILED,
    /** Envoi volontairement ignoré (règle inactive, préférence opt-out ou coordonnée absente). */
    SKIPPED,
    /** Notification consultée et acquittée par le destinataire. */
    READ,
    /** Alias historique normalisé en {@link #READ} en base de données. */
    @Deprecated(forRemoval = false)
    LUE
}
