package com.sodepa.erp.comptabilite.generale.application.usecase;

import com.sodepa.erp.comptabilite.generale.application.inputs.SimulationTvaInput;
import com.sodepa.erp.comptabilite.generale.application.outputs.SimulationTvaResponse;
import com.sodepa.erp.comptabilite.generale.infrastructure.adapter.EcritureAdapter;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UseCase pour simuler les lignes d'écriture comptable pour une opération soumise à la TVA.
 */
@Service
@RequiredArgsConstructor
public class SimulerTvaUseCase implements UseCase<SimulationTvaInput, SimulationTvaResponse> {

    private final EcritureAdapter adapter;

    @Override
    @Transactional
    public SimulationTvaResponse execute(SimulationTvaInput input) {
        return adapter.simulerTva(input);
    }
}
