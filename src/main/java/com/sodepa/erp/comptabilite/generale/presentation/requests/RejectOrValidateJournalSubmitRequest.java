package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerStatus;
import com.sodepa.erp.utils.MakerCheckerOperationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Requête de validation ou rejet d'une soumission de journal comptable.
 */
@Builder
public record RejectOrValidateJournalSubmitRequest(
        @NotNull MakerCheckerStatus decision,
        @NotBlank String notes,
        @NotNull MakerCheckerOperationType checkerOperationType
) {
}
