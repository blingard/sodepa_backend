package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.MakerCheckerOperationType;
import com.sodepa.erp.utils.MakerCheckerStatus;
import lombok.Builder;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `ValidateOrRejectSubmissionInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Builder
public record ValidateOrRejectSubmissionInput(
        UUID id,
        MakerCheckerStatus decision,
        String notes,
        MakerCheckerOperationType checkerOperationType
) {
}
