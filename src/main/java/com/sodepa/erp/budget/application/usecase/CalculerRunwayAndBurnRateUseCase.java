package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.RunwayReportOutput;
import com.sodepa.erp.budget.infrastructure.adapter.PilotageAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculerRunwayAndBurnRateUseCase implements UseCase<Void, RunwayReportOutput> {
    private final PilotageAdapter adapter;

    @Override
    public RunwayReportOutput execute(Void input) {
        return adapter.calculerRunwayAndBurnRate();
    }
}
