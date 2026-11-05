package com.sodepa.erp.paie.presentation.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerPeriodePaieRequest {

    @NotNull(message = "L'année est obligatoire")
    @Min(value = 2020, message = "Année invalide")
    private Integer annee;

    @NotNull(message = "Le mois est obligatoire")
    @Min(value = 1, message = "Mois entre 1 et 12")
    @Max(value = 12, message = "Mois entre 1 et 12")
    private Integer mois;
}
