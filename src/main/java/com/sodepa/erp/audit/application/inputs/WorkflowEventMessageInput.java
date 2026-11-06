package com.sodepa.erp.audit.application.inputs;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO d'événement d'audit pour les actions de workflow persistées dans ClickHouse.
 */
@Builder
public record WorkflowEventMessageInput(
        UUID id,
        UUID instanceId,
        String metierCode,
        UUID definitionId,
        UUID etapeId,
        UUID tacheId,
        UUID acteurId,
        UUID employeId,
        String matricule,
        String userId,
        String username,
        UUID uniteId,
        String actionType,
        String decision,
        String commentaire,
        String payload,
        LocalDateTime timestamp
) {}
