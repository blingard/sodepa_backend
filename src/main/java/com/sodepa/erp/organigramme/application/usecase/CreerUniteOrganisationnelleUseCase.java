package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CreerUniteInput;
import com.sodepa.erp.organigramme.application.outputs.UniteOrganisationnelleOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreerUniteOrganisationnelleUseCase implements UseCase<CreerUniteInput, UniteOrganisationnelleOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public UniteOrganisationnelleOutput execute(CreerUniteInput input) {
        return organigrammePort.creerUnite(input);
    }
}
