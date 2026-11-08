package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.application.outputs.AuditTrailOutput;
import com.sodepa.erp.budget.infrastructure.adapter.PilotageAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `ConsulterAuditTrailBudgetUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class ConsulterAuditTrailBudgetUseCase implements UseCase<ConsulterAuditTrailBudgetUseCase.Input, List<AuditTrailOutput>> {
    public record Input(String entiteNom, UUID entiteId) {}
    private final PilotageAdapter adapter;

    @Override
    public List<AuditTrailOutput> execute(Input input) {
        return adapter.consulterAuditTrail(input.entiteNom(), input.entiteId());
    }
}
