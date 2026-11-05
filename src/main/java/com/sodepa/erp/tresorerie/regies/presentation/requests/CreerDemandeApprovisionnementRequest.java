package com.sodepa.erp.tresorerie.regies.presentation.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreerDemandeApprovisionnementRequest {

    @NotNull(message = "L'identifiant de la régie est obligatoire")
    private UUID regieId;

    @NotNull(message = "Le montant d'approvisionnement demandé est obligatoire")
    private BigDecimal montantDemandeFcfa;

    @NotBlank(message = "Le motif du réapprovisionnement est obligatoire")
    private String motifReapprovisionnement;

    private String etatJustificatifsDepenses;

    private BigDecimal montantJustificatifsValides;
}
