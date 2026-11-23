package com.sodepa.erp.notification.presentation.dto;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Requête JSON unifiée d'émission multi-canal (Email, SMS, WhatsApp, In-App) via le moteur de règles et templates.
 */
public record DispatchNotificationRequest(
        UUID utilisateurId,
        @NotBlank @Size(max = 150) String destinataireNom,
        @Size(max = 180) String email,
        @Size(max = 40) String telephoneSms,
        @Size(max = 40) String whatsappPhone,
        @Size(max = 250) String sujet,
        @NotBlank String messageCorps,
        @Size(max = 60) String typeEvenement,
        @Size(max = 60) String referenceEntiteType,
        @Size(max = 100) String referenceEntiteId,
        List<CanalNotification> canaux,
        Map<String, String> templateVariables,
        @Size(max = 120) String idempotencyKey
) {
}
