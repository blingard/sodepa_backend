package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemanderRallongeStageRequest {

    @NotNull(message = "L'identifiant du stage est obligatoire")
    private UUID stageId;

    @NotNull(message = "La nouvelle date de fin souhaitée est obligatoire")
    private LocalDate nouvelleDateFin;

    private String motif;
}
