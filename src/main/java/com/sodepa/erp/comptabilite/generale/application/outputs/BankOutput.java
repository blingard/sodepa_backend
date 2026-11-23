package com.sodepa.erp.comptabilite.generale.application.outputs;

import java.util.List;
import java.util.UUID;

/**
 * Composant applicatif `BankOutput` du système ERP SODEPA.
 */
public record BankOutput(
        UUID id,
        String code,
        String nom,
        String accountingCode,
        String logo,
        boolean status,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) {
    public BankOutput(
            UUID id,
            String code,
            String nom,
            String accountingCode,
            String logo,
            boolean status
    ) {
        this(id, code, nom, accountingCode, logo, status, false, false, null, null, null, null);
    }
}
