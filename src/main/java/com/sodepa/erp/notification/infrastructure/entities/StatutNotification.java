package com.sodepa.erp.notification.infrastructure.entities;

/**
 * Statut du cycleée d'acheminement d'une notification (Email, SMS ou interne).
 */
public enum StatutNotification {
    /** Notification enregistrée et en file d'attente d'expédition. */
    EN_ATTENTE,
    /** Notification transmise avec succès à la passerelle SMTP ou SMS. */
    ENVOYEE,
    /** Échec d'acheminement technique (trace d'erreur conservée pour relance). */
    ECHEC,
    /** Notification consultée et acquittée par le destinataire. */
    LUE
}
