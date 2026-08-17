package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerStatus;

import java.util.UUID;

/**
 * Input DTO for validating or rejecting a Maker-Checker submission.
 */
public record ValidateOrRejectMakerSubmitInput(
        UUID requestId,
        MakerCheckerStatus decision,
        String notes
) {
}
