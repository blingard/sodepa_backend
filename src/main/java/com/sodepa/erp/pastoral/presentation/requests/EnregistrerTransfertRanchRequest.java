package com.sodepa.erp.pastoral.presentation.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerTransfertRanchRequest {

    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    @NotNull(message = "Le ranch de destination est obligatoire")
    private UUID ranchDestinationId;

    private LocalDate dateTransfert;

    private String justificatifRef;

    private String observations;
}
