package com.sodepa.erp.comptabilite.generale.infrastructure.event;

import com.sodepa.erp.utils.CodeJournal;
import lombok.Builder;
import java.util.UUID;

/**
 * Payload sérialisé dans le système Maker-Checker pour les opérations sur les journaux.
 */
@Builder
public record JournalEventInput(
        UUID id,
        CodeJournal code,
        String intitule,
        String typeJournal,
        Boolean actif,
        String userId
) {
}
