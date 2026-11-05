package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscrireEmployeFormationRequest {

    @NotNull(message = "L'identifiant de l'action est obligatoire")
    private UUID actionId;

    @NotNull(message = "L'identifiant de l'employé est obligatoire")
    private UUID employeId;

    private BigDecimal noteEvaluationChaud;

    private boolean attestationDelivree;

    private String avisHierarchique;
}
