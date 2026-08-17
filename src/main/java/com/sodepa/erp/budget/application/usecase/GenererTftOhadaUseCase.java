package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.TftOhadaReportOutput;
import com.sodepa.erp.budget.infrastructure.adapter.PilotageAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenererTftOhadaUseCase implements UseCase<Integer, TftOhadaReportOutput> {
    private final PilotageAdapter adapter;

    @Override
    public TftOhadaReportOutput execute(Integer input) {
        return adapter.genererTftOhada(input);
    }
}
