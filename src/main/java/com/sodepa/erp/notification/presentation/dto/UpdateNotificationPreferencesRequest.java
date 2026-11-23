package com.sodepa.erp.notification.presentation.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Requête de mise à jour des préférences personnelles de réception des notifications.
 */
public record UpdateNotificationPreferencesRequest(
        Boolean emailEnabled,
        Boolean smsEnabled,
        Boolean whatsappEnabled,
        Boolean inAppEnabled,
        @Size(max = 40) String whatsappPhone,
        @Pattern(regexp = "^(fr|en)$", message = "Langue supportée uniquement : fr ou en.") String langue
) {
}
