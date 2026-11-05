package com.sodepa.erp.rh.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprobationContratDgRequest {
    @NotBlank(message = "La référence de la Note de Service ou Circulaire du DG est obligatoire")
    private String referenceNoteServiceDg;

    private LocalDate dateSignature;

    private String commentaire;
}
