package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.RapprochementInput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'usage pour le rapprochement automatique.
 */
@Service
@RequiredArgsConstructor
public class EffectuerRapprochementAutomatiqueUseCase implements UseCase<RapprochementInput, Integer> {

    private final RapprochementAdapter rapprochementAdapter;

    @Override
    @Transactional
    public Integer execute(RapprochementInput input) {
        return rapprochementAdapter.effectuerRapprochementAutomatique(input);
    }
}
