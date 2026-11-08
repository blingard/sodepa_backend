package com.sodepa.erp.comptabilite.generale.application.inputs;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `UpdateBankImageInput` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
public record UpdateBankImageInput(

        @NotNull UUID id,
        @NotNull MultipartFile logo
        ) {
}
