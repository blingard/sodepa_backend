package com.sodepa.erp.share;

import com.sodepa.erp.user.application.outputs.UserRecordSmartOutput;
import lombok.Builder;
import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Output DTO for Maker-Checker records.
 */
@Builder
public record MakerCheckerOutput(
        UUID id,
        MakerCheckerEntityName entityName,
        String entityPk,
        UserRecordSmartOutput maker,
        UserRecordSmartOutput checker,
        MakerCheckerStatus status,
        Map<String, Object> payload,
        String notes,
        Instant createdAt,
        Instant decidedAt,
        Instant expiredAt,
        MakerCheckerOperationType checkerOperationType
) {
}
