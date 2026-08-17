package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.JournalAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cas d'utilisation pour basculer le statut actif d'un journal.
 */
@Component
@RequiredArgsConstructor
public class ToggleActiveJournalUseCase implements UseCase<UUID, Void> {

    private final JournalAdapter journalAdapter;

    @Override
    public Void execute(UUID input) {
        journalAdapter.toggleActive(input);
        return null;
    }
}
