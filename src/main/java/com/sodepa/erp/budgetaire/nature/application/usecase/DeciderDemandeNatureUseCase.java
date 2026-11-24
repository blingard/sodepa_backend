package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.commun.DeciderDemandeInput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Valide ou rejette une demande portant sur une nature. */
@Service
@RequiredArgsConstructor
public class DeciderDemandeNatureUseCase implements UseCase<DeciderDemandeInput, Void> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public Void execute(DeciderDemandeInput input) {
        adapter.decider(input);
        return null;
    }
}
