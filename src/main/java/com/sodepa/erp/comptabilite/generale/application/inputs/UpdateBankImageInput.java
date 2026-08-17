package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public record UpdateBankImageInput(

        @NotNull UUID id,
        @NotNull MultipartFile logo
        ) {
}
