package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.MakerCheckerEntityName;
import com.sodepa.erp.utils.MakerCheckerOperationType;

import java.util.UUID;

/**
 * Summary DTO for Maker-Checker records.
 */
public record MakerCheckerSmartOutput(
        UUID id,
        MakerCheckerEntityName entityName,
        String entityPk,
        MakerCheckerStatus status,
        MakerCheckerOperationType checkerOperationType
) {
}
