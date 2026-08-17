package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.CreateJournalInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cas d'utilisation pour la création d'un journal.
 */
@Component
@RequiredArgsConstructor
public class CreateJournalUseCase implements UseCase<CreateJournalInput, Void> {

    private final JournalAdapter journalAdapter;

    @Override
    public Void execute(CreateJournalInput input) {
        journalAdapter.initCreateJournal(input);
        return null;
    }
}
