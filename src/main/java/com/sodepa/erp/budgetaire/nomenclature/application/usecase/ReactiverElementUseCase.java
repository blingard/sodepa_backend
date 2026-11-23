package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ChangerEtatElementInput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la réactivation d'un élément désactivé. */
@Service
@RequiredArgsConstructor
public class ReactiverElementUseCase implements UseCase<ChangerEtatElementInput, SubmissionOutput> {

    private final NomenclatureAdapter adapter;

    @Override
    public SubmissionOutput execute(ChangerEtatElementInput input) {
        return adapter.initReactiver(input);
    }
}
