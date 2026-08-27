package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.inputs.PeriodeInput;
import com.sodepa.erp.budget.application.outputs.PrevisionTresorerieOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** UseCase pour lister les prévisions de trésorerie d'une période. */
@Service
@RequiredArgsConstructor
public class ListerPrevisionsUseCase implements UseCase<PeriodeInput, List<PrevisionTresorerieOutput>> {
    private final TresorerieAdapter adapter;

    @Override
    public List<PrevisionTresorerieOutput> execute(PeriodeInput input) {
        return adapter.listerPrevisions(input);
    }
}
