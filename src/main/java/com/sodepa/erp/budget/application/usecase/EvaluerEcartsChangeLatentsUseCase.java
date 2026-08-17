package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.ValuationCouvertureReportOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EvaluerEcartsChangeLatentsUseCase implements UseCase<EvaluerEcartsChangeLatentsUseCase.Input, ValuationCouvertureReportOutput> {
    public record Input(UUID contratId, BigDecimal coursSpotActuel) {}
    private final TresorerieAdapter adapter;

    @Override
    public ValuationCouvertureReportOutput execute(Input input) {
        return adapter.evaluerEcartsDeChangeLatents(input.contratId(), input.coursSpotActuel());
    }
}
