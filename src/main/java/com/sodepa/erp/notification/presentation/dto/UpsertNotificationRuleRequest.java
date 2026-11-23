package com.sodepa.erp.notification.presentation.dto;

import com.sodepa.erp.notification.infrastructure.entities.CanalNotification;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requête d'administration pour créer ou modifier une règle événement × canal.
 */
public record UpsertNotificationRuleRequest(
        @NotBlank @Size(max = 60) String typeEvenement,
        @NotNull CanalNotification canal,
        boolean actif,
        boolean critique,
        @Min(1) @Max(1000) Integer ordrePriorite,
        CanalNotification fallbackCanal,
        @Size(max = 255) String description
) {
}
