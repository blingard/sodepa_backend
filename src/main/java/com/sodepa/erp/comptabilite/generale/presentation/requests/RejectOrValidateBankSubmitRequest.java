package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RejectOrValidateBankSubmitRequest(
        @NotNull MakerCheckerStatus decision,
        @NotBlank String notes,
        @NotNull MakerCheckerOperationType checkerOperationType
) {
}
