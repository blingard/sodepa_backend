package com.sodepa.erp.budgetaire.nature.application.usecase;

import com.sodepa.erp.budgetaire.nature.application.outputs.NatureBudgetaireOutput;
import com.sodepa.erp.budgetaire.nature.infrastructure.adapter.NatureBudgetaireAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Fiche d'une nature budgétaire. */
@Service
@RequiredArgsConstructor
public class ConsulterNatureUseCase implements UseCase<UUID, NatureBudgetaireOutput> {

    private final NatureBudgetaireAdapter adapter;

    @Override
    public NatureBudgetaireOutput execute(UUID input) {
        return adapter.consulter(input);
    }
}
