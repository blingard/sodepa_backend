package com.sodepa.erp.budgetaire.nomenclature.application.usecase;

import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.nomenclature.application.outputs.ElementProgrammatiqueOutput;
import com.sodepa.erp.budgetaire.nomenclature.infrastructure.adapter.NomenclatureAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Demandes sur la nomenclature en attente de décision, hors celles de l'appelant. */
@Service
@RequiredArgsConstructor
public class ListerDemandesNomenclatureEnAttenteUseCase implements UseCase<Pageable, PageRecord<DemandeBudgetaireOutput<ElementProgrammatiqueOutput>>> {

    private final NomenclatureAdapter adapter;

    @Override
    public PageRecord<DemandeBudgetaireOutput<ElementProgrammatiqueOutput>> execute(Pageable input) {
        return adapter.demandesEnAttente(input);
    }
}
