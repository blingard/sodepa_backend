package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.TypeTiers;
import java.util.List;
import java.util.UUID;

/**
 * Vue allégée d'un tiers pour les listes et sélections.
 */
public record TiersSmartOutput(
        UUID id,
        String code,
        String raisonSociale,
        TypeTiers typeTiers,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) {
    public TiersSmartOutput(
            UUID id,
            String code,
            String raisonSociale,
            TypeTiers typeTiers
    ) {
        this(id, code, raisonSociale, typeTiers, false, false, null, null, null, null);
    }
}
