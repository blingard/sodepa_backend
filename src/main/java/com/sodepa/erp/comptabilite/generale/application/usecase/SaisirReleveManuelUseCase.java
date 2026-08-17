package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.ReleveManuelInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.ReleveBancaireOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'usage pour la saisie manuelle d'un relevé bancaire.
 */
@Service
@RequiredArgsConstructor
public class SaisirReleveManuelUseCase implements UseCase<ReleveManuelInput, ReleveBancaireOutput> {

    private final RapprochementAdapter rapprochementAdapter;

    @Override
    @Transactional
    public ReleveBancaireOutput execute(ReleveManuelInput input) {
        return rapprochementAdapter.saisirReleveManuel(input);
    }
}
