package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.comptabilite.generale.application.usecase.EcritureValidatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `ExecuterRapprochementBudgetaireUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class ExecuterRapprochementBudgetaireUseCase {
    private final RapprochementAdapter adapter;

    @EventListener
    public void traiterRapprochementBudget(EcritureValidatedEvent event) {
        adapter.traiterRapprochementBudget(event);
    }
}
