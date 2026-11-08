package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.AffecterCollaborateurInput;
import com.sodepa.erp.organigramme.application.outputs.AffectationOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `AffecterCollaborateurUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class AffecterCollaborateurUseCase implements UseCase<AffecterCollaborateurInput, AffectationOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public AffectationOutput execute(AffecterCollaborateurInput input) {
        return organigrammePort.affecterCollaborateur(input);
    }
}
