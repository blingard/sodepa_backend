package com.sodepa.erp.configuration.parametres.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateParametreRequest {

    @NotBlank(message = "La nouvelle valeur est obligatoire")
    private String valeur;

    private String modifiePar;
}
