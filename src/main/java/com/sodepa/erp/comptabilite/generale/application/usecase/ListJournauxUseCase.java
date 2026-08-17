package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.JournalSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cas d'utilisation pour lister tous les journaux.
 */
@Component
@RequiredArgsConstructor
public class ListJournauxUseCase implements UseCase<Void, List<JournalSmartOutput>> {

    private final JournalAdapter journalAdapter;

    @Override
    public List<JournalSmartOutput> execute(Void input) {
        return journalAdapter.listAllJournaux();
    }
}
