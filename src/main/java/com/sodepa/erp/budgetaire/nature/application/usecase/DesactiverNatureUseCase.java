package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la désactivation d'une nature. */
@Service
@RequiredArgsConstructor
public class DesactiverNatureUseCase implements UseCase<ChangerEtatInput, SubmissionOutput> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public SubmissionOutput execute(ChangerEtatInput input) {
        return adapter.initDesactiver(input);
    }
}
