package com.sodepa.erp.formation.presentation.requests;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Objet de transfert de données (DTO / Requête) pour l'opération `InscrireEmployeFormationRequest` dans l'ERP SODEPA.
 * Encapsule les paramètres requis avec leurs règles de validation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InscrireEmployeFormationRequest {

    /**
     * Identifiant unique technique (UUID) de l'élément Action métier ou technique déclenchée associé.
     */
    @NotNull(message = "L'identifiant de l'action est obligatoire")
    private UUID actionId;

    /**
     * Identifiant unique de l'employé concerné dans le cadre des opérations SODEPA.
     */
    @NotNull(message = "L'identifiant de l'employé est obligatoire")
    private UUID employeId;

    /**
     * Champ noteEvaluationChaud : note evaluation chaud utilisé pour le pilotage et le suivi dans InscrireEmployeFormationRequest.
     */
    private BigDecimal noteEvaluationChaud;

    /**
     * Champ attestationDelivree : attestation delivree utilisé pour le pilotage et le suivi dans InscrireEmployeFormationRequest.
     */
    private boolean attestationDelivree;

    /**
     * Champ avisHierarchique : avis hierarchique utilisé pour le pilotage et le suivi dans InscrireEmployeFormationRequest.
     */
    private String avisHierarchique;
}
