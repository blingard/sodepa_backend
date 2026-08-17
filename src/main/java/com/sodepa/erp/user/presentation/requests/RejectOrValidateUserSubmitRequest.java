package com.sodepa.erp.user.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête pour valider ou rejeter une soumission.
 */
public record RejectOrValidateUserSubmitRequest(
        @NotNull MakerCheckerStatus decision,
        @NotBlank String notes,
        @NotNull MakerCheckerOperationType checkerOperationType
) {
}
