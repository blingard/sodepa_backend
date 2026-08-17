package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.SyncInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'usage pour la synchronisation automatique d'un relevé bancaire.
 */
@Service
@RequiredArgsConstructor
public class SynchroniserReleveAutomatiqueUseCase implements UseCase<SyncInput, ReleveBancaireOutput> {

    private final RapprochementAdapter rapprochementAdapter;

    @Override
    @Transactional
    public ReleveBancaireOutput execute(SyncInput input) {
        return rapprochementAdapter.synchroniserReleveAutomatique(input);
    }
}
