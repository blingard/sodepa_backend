package com.sodepa.erp.pastoral.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerChangementCategorieRequest {

    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    @NotBlank(message = "La nouvelle catégorie est obligatoire")
    private String nouvelleCategorieCode;

    private LocalDate dateChangement;

    private String justificatifRef;

    private String observations;
}
