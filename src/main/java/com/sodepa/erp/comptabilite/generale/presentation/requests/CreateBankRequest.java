package com.sodepa.erp.comptabilite.generale.presentation.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `CreateBankRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record CreateBankRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String accountAccountingCode
) {
}
