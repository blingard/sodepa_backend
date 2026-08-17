package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.UpdateJournalInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cas d'utilisation pour la mise à jour d'un journal.
 */
@Component
@RequiredArgsConstructor
public class UpdateJournalUseCase implements UseCase<UpdateJournalInput, Void> {

    private final JournalAdapter journalAdapter;

    @Override
    public Void execute(UpdateJournalInput input) {
        journalAdapter.updateJournal(input);
        return null;
    }
}
