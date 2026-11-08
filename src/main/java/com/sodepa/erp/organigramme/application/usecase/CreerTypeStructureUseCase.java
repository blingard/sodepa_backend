package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CreerTypeStructureInput;
import com.sodepa.erp.organigramme.application.outputs.TypeStructureOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `CreerTypeStructureUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class CreerTypeStructureUseCase implements UseCase<CreerTypeStructureInput, TypeStructureOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public TypeStructureOutput execute(CreerTypeStructureInput input) {
        return organigrammePort.creerTypeStructure(input);
    }
}
