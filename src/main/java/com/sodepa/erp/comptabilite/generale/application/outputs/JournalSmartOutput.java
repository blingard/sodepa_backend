package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.CodeJournal;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

/**
 * Vue allégée d'un journal comptable.
 */
@Builder
public record JournalSmartOutput(
        UUID id,
        CodeJournal code,
        String intitule,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) {
    public JournalSmartOutput(
            UUID id,
            CodeJournal code,
            String intitule
    ) {
        this(id, code, intitule, false, false, null, null, null, null);
    }
}
