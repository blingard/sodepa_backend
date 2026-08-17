package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record CreateBankInput(

        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountAccountingCode,
        @NotNull MultipartFile logo
        ) {
}
