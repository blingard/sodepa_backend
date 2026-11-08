package com.sodepa.erp.notification.infrastructure.entities;

/**
 * Énumération des canaux de diffusion de notifications supportés par le socleée transversal SODEPA (CT-REF-05).
 * <ul>
 *   <li>{@link #EMAIL} : Envoi d'un courrier électronique SMTP au collaborateur ou partenaire.</li>
 *   <li>{@link #SMS} : Envoi d'un message texte court (SMS) sur le numéro mobile de l'agent ou du tiers.</li>
 *   <li>{@link #EMAIL_ET_SMS} : Diffusion simultanée par courrier électronique et par SMS (recommandé pour les circuits de validation et alertes critiques).</li>
 *   <li>{@link #IN_APP} : Notification interne consultable dans le centre de notifications de l'ERP.</li>
 * </ul>
 */
public enum CanalNotification {
    EMAIL,
    SMS,
    EMAIL_ET_SMS,
    IN_APP
}
