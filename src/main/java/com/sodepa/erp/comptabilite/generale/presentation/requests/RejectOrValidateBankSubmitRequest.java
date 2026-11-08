package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `RejectOrValidateBankSubmitRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record RejectOrValidateBankSubmitRequest(
        @NotNull MakerCheckerStatus decision,
        @NotBlank String notes,
        @NotNull MakerCheckerOperationType checkerOperationType
) {
}
