package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateBankInput(

        @NotNull UUID id,
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountingCode,
        @NotBlank String logo,
        boolean status
        ) {
}
