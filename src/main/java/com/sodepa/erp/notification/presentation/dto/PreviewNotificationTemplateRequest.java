package com.sodepa.erp.notification.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Requête d'aperçu sécurisé d'un modèle avant publication.
 */
public record PreviewNotificationTemplateRequest(
        @NotBlank @Size(max = 80) String codeOrTypeEvenement,
        @Size(max = 10) String langue,
        Map<String, String> variables
) {
}
