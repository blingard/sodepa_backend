package com.sodepa.erp.budget.application.usecase;

import com.sodepa.erp.budget.infrastructure.adapter.RapprochementAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `MatchingAutomatiqueUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class MatchingAutomatiqueUseCase implements UseCase<UUID, Integer> {
    private final RapprochementAdapter adapter;

    @Override
    public Integer execute(UUID input) {
        return adapter.matchingAutomatique(input);
    }
}
