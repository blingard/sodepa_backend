package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.CodeJournal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Entrée pour la création d'un journal comptable.
 */
@Builder
public record CreateJournalInput(
        @NotNull CodeJournal code,
        @NotBlank String intitule,
        @NotBlank String typeJournal
) {
}
