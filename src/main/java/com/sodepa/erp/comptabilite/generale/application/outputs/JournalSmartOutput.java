package com.sodepa.erp.comptabilite.generale.application.outputs;

import com.sodepa.erp.utils.CodeJournal;
import lombok.Builder;
import java.util.UUID;

/**
 * Vue allégée d'un journal comptable.
 */
@Builder
public record JournalSmartOutput(
        UUID id,
        CodeJournal code,
        String intitule
) {
}
