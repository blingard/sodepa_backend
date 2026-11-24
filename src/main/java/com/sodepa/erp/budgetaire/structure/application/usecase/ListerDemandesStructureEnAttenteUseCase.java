package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.commun.DemandeBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.application.outputs.StructureBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.utils.PageRecord;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Demandes sur les structures en attente de décision, hors celles de l'appelant. */
@Service
@RequiredArgsConstructor
public class ListerDemandesStructureEnAttenteUseCase implements UseCase<Pageable, PageRecord<DemandeBudgetaireOutput<StructureBudgetaireOutput>>> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public PageRecord<DemandeBudgetaireOutput<StructureBudgetaireOutput>> execute(Pageable input) {
        return adapter.demandesEnAttente(input);
    }
}
