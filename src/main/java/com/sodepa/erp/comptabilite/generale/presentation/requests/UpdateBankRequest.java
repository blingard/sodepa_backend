package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;

public record UpdateBankRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountingCode,
        @NotBlank String logo,
        boolean status
) {
}
