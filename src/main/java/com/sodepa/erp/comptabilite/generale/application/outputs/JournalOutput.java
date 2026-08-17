package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.CodeJournal;
import lombok.Builder;
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
        Boolean actif
) {
}
