package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CreerFinancementInput;
import com.sodepa.erp.budget.application.outputs.FinancementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.FinancementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EnregistrerFinancementUseCase implements UseCase<CreerFinancementInput, FinancementOutput> {

    private final FinancementAdapter financementAdapter;

    @Override
    public FinancementOutput execute(CreerFinancementInput input) {
        return financementAdapter.enregistrerFinancement(input);
    }
}
