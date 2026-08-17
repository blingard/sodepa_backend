package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Requête pour valider ou rejeter la soumission d'une opération sur un compte.
 */
@Builder
public record RejectOrValidateCompteSubmitRequest(
    @NotNull MakerCheckerStatus decision,
    @NotBlank String notes,
    @NotNull MakerCheckerOperationType checkerOperationType
) {
}
