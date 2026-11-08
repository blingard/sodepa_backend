package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `UpdateBankInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record UpdateBankInput(

        @NotNull UUID id,
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountingCode,
        @NotBlank String logo,
        boolean status
        ) {
}
