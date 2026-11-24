package com.sodepa.erp.budgetaire.source.application.usecase;

import com.sodepa.erp.budgetaire.source.application.outputs.SourceFinancementOutput;
import com.sodepa.erp.budgetaire.source.infrastructure.adapter.SourceFinancementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Fiche d'une source de financement. */
@Service
@RequiredArgsConstructor
public class ConsulterSourceUseCase implements UseCase<UUID, SourceFinancementOutput> {

    private final SourceFinancementAdapter adapter;

    @Override
    public SourceFinancementOutput execute(UUID input) {
        return adapter.consulter(input);
    }
}
