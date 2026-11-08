package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreateBankInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreateBankInput(

        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountAccountingCode,
        @NotNull MultipartFile logo
        ) {
}
