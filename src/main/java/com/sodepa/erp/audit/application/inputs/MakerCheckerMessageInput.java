package com.sodepa.erp.audit.application.inputs;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `MakerCheckerMessageInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record MakerCheckerMessageInput(
        UUID id,
        String entityName,
        String entityPk,
        String payload,
        String maker_id,
        String checker_id,
        LocalDateTime timestamp
) {
}
