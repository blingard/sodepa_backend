package com.sodepa.erp.pastoral.presentation.requests;

import com.sodepa.erp.pastoral.infrastructure.entities.EtatSanteBovin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnregistrerPeseeRequest {

    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    @NotNull(message = "Le poids mesuré est obligatoire")
    private BigDecimal poidsKg;

    private LocalDate datePesee;

    private EtatSanteBovin etatSante;

    private String observations;
}
