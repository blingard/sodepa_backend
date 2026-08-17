package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.BfrReportOutput;
import com.sodepa.erp.budget.infrastructure.adapter.TresorerieAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CalculerBfrUseCase implements UseCase<LocalDate, BfrReportOutput> {
    private final TresorerieAdapter adapter;

    @Override
    public BfrReportOutput execute(LocalDate input) {
        return adapter.calculerBFR(input);
    }
}
