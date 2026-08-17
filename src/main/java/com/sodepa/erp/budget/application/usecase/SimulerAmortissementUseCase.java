package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.CreerFinancementInput;
import com.sodepa.erp.budget.application.outputs.EcheanceOutput;
import com.sodepa.erp.budget.infrastructure.adapter.FinancementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimulerAmortissementUseCase implements UseCase<CreerFinancementInput, List<EcheanceOutput>> {

    private final FinancementAdapter financementAdapter;

    @Override
    public List<EcheanceOutput> execute(CreerFinancementInput input) {
        return financementAdapter.genererPlanAmortissement(
                input.capital(), input.tauxNominal(), input.dureeMois(), input.periodicite(), input.dateEffet());
    }
}
