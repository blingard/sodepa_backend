package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.nomenclature.application.inputs.DeciderDemandeNomenclatureInput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Valide ou rejette une demande portant sur la nomenclature. */
@Service
@RequiredArgsConstructor
public class DeciderDemandeNomenclatureUseCase implements UseCase<DeciderDemandeNomenclatureInput, Void> {

    private final NomenclatureAdapter adapter;

    @Override
    public Void execute(DeciderDemandeNomenclatureInput input) {
        adapter.decider(input);
        return null;
    }
}
