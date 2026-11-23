package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.nomenclature.application.inputs.CreerElementInput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la création d'un programme ou d'une action. */
@Service
@RequiredArgsConstructor
public class CreerElementUseCase implements UseCase<CreerElementInput, SubmissionOutput> {

    private final NomenclatureAdapter adapter;

    @Override
    public SubmissionOutput execute(CreerElementInput input) {
        return adapter.initCreer(input);
    }
}
