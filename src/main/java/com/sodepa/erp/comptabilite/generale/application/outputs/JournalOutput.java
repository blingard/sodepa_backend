package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.CodeJournal;
import lombok.Builder;
import java.util.List;
import java.util.UUID;

/**
 * Vue complète d'un journal comptable.
 */
@Builder
public record JournalOutput(
        UUID id,
        CodeJournal code,
        String intitule,
        String typeJournal,
        Boolean actif,
        boolean enCoursDeModification,
        boolean workflowEnCours,
        UUID workflowInstanceId,
        String workflowStatut,
        String workflowOperationType,
        List<String> piecesJointesWorkflow
) {
    public JournalOutput(
            UUID id,
            CodeJournal code,
            String intitule,
            String typeJournal,
            Boolean actif
    ) {
        this(id, code, intitule, typeJournal, actif, false, false, null, null, null, null);
    }
}
