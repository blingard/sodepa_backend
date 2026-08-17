package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.JournalOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cas d'utilisation pour obtenir un journal par son identifiant.
 */
@Component
@RequiredArgsConstructor
public class GetJournalByIdUseCase implements UseCase<UUID, JournalOutput> {

    private final JournalAdapter journalAdapter;

    @Override
    public JournalOutput execute(UUID input) {
        return journalAdapter.getJournalById(input);
    }
}
