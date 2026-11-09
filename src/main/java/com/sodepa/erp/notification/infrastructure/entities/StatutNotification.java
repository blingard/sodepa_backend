package com.sodepa.erp.notification.infrastructure.entities;

/**
 * Statut du cycleée d'acheminement d'une notification (Email, SMS ou interne).
 */
public enum StatutNotification {
    /** Notification enregistrée et en file d'attente d'expédition. */
    PENDING,
    /** Notification transmise avec succès à la passerelle SMTP ou SMS. */
    SEND,
    /** Échec d'acheminement technique (trace d'erreur conservée pour relance). */
    FAILED,
    /** Notification consultée et acquittée par le destinataire. */
    READ,
    LUE
}
