package com.sodepa.erp.comptabilite.generale.presentation.requests;

import com.sodepa.erp.utils.CodeJournal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * Requête de création d'un journal comptable.
 */
@Builder
public record CreateJournalRequest(
        @NotNull CodeJournal code,
        @NotBlank String intitule,
        @NotBlank String typeJournal
) {
}
