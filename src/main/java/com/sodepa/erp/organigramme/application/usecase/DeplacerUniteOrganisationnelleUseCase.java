package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.DeplacerUniteInput;
import com.sodepa.erp.organigramme.application.outputs.UniteOrganisationnelleOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeplacerUniteOrganisationnelleUseCase implements UseCase<DeplacerUniteInput, UniteOrganisationnelleOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public UniteOrganisationnelleOutput execute(DeplacerUniteInput input) {
        return organigrammePort.deplacerUnite(input);
    }
}
