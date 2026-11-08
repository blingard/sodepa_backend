package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CloturerAffectationInput;
import com.sodepa.erp.organigramme.application.outputs.AffectationOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `CloturerAffectationUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class CloturerAffectationUseCase implements UseCase<CloturerAffectationInput, AffectationOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public AffectationOutput execute(CloturerAffectationInput input) {
        return organigrammePort.cloturerAffectation(input);
    }
}
