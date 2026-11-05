package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerPlanFormationRequest {

    @NotNull(message = "L'année d'exercice est obligatoire")
    private Integer anneeExercice;

    @NotBlank(message = "L'intitulé du plan est obligatoire")
    private String intitule;

    private BigDecimal budgetTotalPrevu;

    private String observations;
}
