package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.RecommandationPaiementOutput;
import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommanderArbitrageDecaissementsUseCase implements UseCase<RecommanderArbitrageDecaissementsUseCase.Input, List<RecommandationPaiementOutput>> {
    public record Input(BigDecimal fondsSecurite, LocalDate debut, LocalDate fin, BigDecimal soldeActuel) {}
    private final RapprochementAdapter adapter;

    @Override
    public List<RecommandationPaiementOutput> execute(Input input) {
        return adapter.recommanderArbitrageDecaissements(input.fondsSecurite(), input.debut(), input.fin(), input.soldeActuel());
    }
}
