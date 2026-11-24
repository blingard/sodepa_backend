package com.sodepa.erp.budgetaire.structure.application.usecase;

import com.sodepa.erp.budgetaire.structure.application.outputs.StructureBudgetaireOutput;
import com.sodepa.erp.budgetaire.structure.infrastructure.adapter.StructureBudgetaireAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Fiche d'une structure budgétaire. */
@Service
@RequiredArgsConstructor
public class ConsulterStructureUseCase implements UseCase<UUID, StructureBudgetaireOutput> {

    private final StructureBudgetaireAdapter adapter;

    @Override
    public StructureBudgetaireOutput execute(UUID input) {
        return adapter.consulter(input);
    }
}
