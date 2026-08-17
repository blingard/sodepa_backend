package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.JournalSmartOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cas d'utilisation pour obtenir un journal actif par son identifiant.
 */
@Component
@RequiredArgsConstructor
public class GetActiveJournalByIdUseCase implements UseCase<UUID, JournalSmartOutput> {

    private final JournalAdapter journalAdapter;

    @Override
    public JournalSmartOutput execute(UUID input) {
        return journalAdapter.getActiveJournalById(input);
    }
}
