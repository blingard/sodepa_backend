package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ValidateOrRejectSubmissionInput(
        UUID id,
        MakerCheckerStatus decision,
        String notes,
        MakerCheckerOperationType checkerOperationType
) {
}
