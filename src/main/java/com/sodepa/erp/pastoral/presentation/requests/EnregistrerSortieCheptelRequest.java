package com.sodepa.erp.pastoral.presentation.requests;

import com.sodepa.erp.pastoral.infrastructure.entities.TypeMouvementCheptel;
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
public class EnregistrerSortieCheptelRequest {

    @NotNull(message = "L'identifiant du bovin est obligatoire")
    private UUID bovinId;

    @NotNull(message = "Le type de sortie est obligatoire")
    private TypeMouvementCheptel typeSortie; // MORTALITE, VENTE_ABATTAGE, REFORME

    private LocalDate dateSortie;

    private BigDecimal montantCession;

    private String justificatifRef;

    private String observations;
}
