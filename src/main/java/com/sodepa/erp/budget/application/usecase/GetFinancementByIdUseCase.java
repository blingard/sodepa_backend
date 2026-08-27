package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.FinancementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.FinancementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** UseCase pour consulter un financement et son échéancier. */
@Service
@RequiredArgsConstructor
public class GetFinancementByIdUseCase implements UseCase<UUID, FinancementOutput> {
    private final FinancementAdapter adapter;

    @Override
    public FinancementOutput execute(UUID input) {
        return adapter.getFinancementById(input);
    }
}
