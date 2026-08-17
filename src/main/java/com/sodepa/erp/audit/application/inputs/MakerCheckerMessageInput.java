package com.sodepa.erp.audit.application.inputs;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

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
