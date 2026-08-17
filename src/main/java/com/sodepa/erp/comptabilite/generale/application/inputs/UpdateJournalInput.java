package com.sodepa.erp.comptabilite.generale.application.inputs;

import com.sodepa.erp.utils.CodeJournal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import java.util.UUID;

/**
 * Entrée pour la mise à jour d'un journal comptable.
 */
@Builder
public record UpdateJournalInput(
        @NotNull UUID id,
        @NotNull CodeJournal code,
        @NotBlank String intitule,
        @NotBlank String typeJournal,
        @NotNull Boolean actif
) {
}
