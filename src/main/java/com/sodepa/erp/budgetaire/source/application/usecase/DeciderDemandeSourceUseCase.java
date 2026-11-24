package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Valide ou rejette une demande portant sur une source de financement. */
@Service
@RequiredArgsConstructor
public class DeciderDemandeSourceUseCase implements UseCase<DeciderDemandeInput, Void> {

    private final SourceFinancementAdapter adapter;

    @Override
    public Void execute(DeciderDemandeInput input) {
        adapter.decider(input);
        return null;
    }
}
