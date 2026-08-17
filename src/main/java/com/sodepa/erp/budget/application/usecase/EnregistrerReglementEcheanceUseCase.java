package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.FinancementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnregistrerReglementEcheanceUseCase implements UseCase<EnregistrerReglementEcheanceUseCase.Input, Void> {

    public record Input(UUID echeanceId, UUID userId) {}

    private final FinancementAdapter financementAdapter;

    @Override
    public Void execute(Input input) {
        financementAdapter.enregistrerPaiementEcheance(input.echeanceId(), input.userId());
        return null;
    }
}
