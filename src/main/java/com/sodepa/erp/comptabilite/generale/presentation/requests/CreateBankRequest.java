package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateBankRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountAccountingCode
) {
}
