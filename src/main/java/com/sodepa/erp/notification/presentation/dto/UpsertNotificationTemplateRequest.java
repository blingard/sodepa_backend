package com.sodepa.erp.notification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Requête d'administration pour créer ou mettre à jour un modèle de notification sécurisé en base.
 */
public record UpsertNotificationTemplateRequest(
        @NotBlank @Size(max = 80) String code,
        @NotBlank @Size(max = 60) String typeEvenement,
        @Pattern(regexp = "^(fr|en)$", message = "Langue supportée uniquement : fr ou en.") String langue,
        @NotBlank @Size(max = 250) String sujetTemplate,
        @NotBlank String bodyTextTemplate,
        String bodyHtmlTemplate,
        @Size(max = 120) String whatsappTemplateName,
        @Size(max = 500) String whatsappParamOrder,
        @Size(max = 500) String allowedPlaceholders,
        Boolean actif
) {
}
