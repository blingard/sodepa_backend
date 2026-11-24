package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.nomenclature.application.inputs.ModifierElementInput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.share.SubmissionOutput;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Soumet la modification du libellé, de la description ou du responsable d'un élément. */
@Service
@RequiredArgsConstructor
public class ModifierElementUseCase implements UseCase<ModifierElementInput, SubmissionOutput> {

    private final NomenclatureAdapter adapter;

    @Override
    public SubmissionOutput execute(ModifierElementInput input) {
        return adapter.initModifier(input);
    }
}
