package com.sodepa.erp.achats.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `RejectOrValidateAchatsSubmitRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record RejectOrValidateAchatsSubmitRequest(
        @NotNull(message = "DECISION_MUST_NOT_BE_NULL") MakerCheckerStatus decision,
        @NotBlank(message = "NOTES_IS_REQUIRED") String notes,
        @NotNull(message = "OPERATION_MUST_NOT_BE_NULL") MakerCheckerOperationType checkerOperationType
) {
}
