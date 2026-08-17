package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.outputs.AmortissementLineOutput;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.ImmobilisationAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * UseCase pour calculer et obtenir le plan d'amortissement prévisionnel.
 */
@Service
@RequiredArgsConstructor
public class GetPlanAmortissementUseCase implements UseCase<UUID, List<AmortissementLineOutput>> {
    private final ImmobilisationAdapter immobilisationAdapter;

    @Override
    public List<AmortissementLineOutput> execute(UUID input) {
        return immobilisationAdapter.getPlanAmortissement(input);
    }
}
