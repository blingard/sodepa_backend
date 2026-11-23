package com.sodepa.erp.notification.infrastructure.entities;

/**
 * Canaux de diffusion supportés par le moteur de notifications ERP SODEPA.
 */
public enum CanalNotification {
    EMAIL,
    SMS,
    WHATSAPP,
    /**
     * Canal historique multi-canaux : conservé uniquement en tant qu'alias d'entrée
     * et automatiquement éclaté en une ligne {@link #EMAIL} et une ligne {@link #SMS}
     * liées par {@code groupId}.
     */
    @Deprecated(forRemoval = false)
    EMAIL_ET_SMS,
    IN_APP
}
