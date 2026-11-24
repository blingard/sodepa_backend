package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.commun.ChangerEtatInput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la réactivation d'une source de financement. */
@Service
@RequiredArgsConstructor
public class ReactiverSourceUseCase implements UseCase<ChangerEtatInput, SubmissionOutput> {

    private final SourceFinancementAdapter adapter;

    @Override
    public SubmissionOutput execute(ChangerEtatInput input) {
        return adapter.initReactiver(input);
    }
}
