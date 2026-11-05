package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CreerVersionOrganigrammeInput;
import com.sodepa.erp.organigramme.application.outputs.OrganigrammeVersionOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreerVersionOrganigrammeUseCase implements UseCase<CreerVersionOrganigrammeInput, OrganigrammeVersionOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public OrganigrammeVersionOutput execute(CreerVersionOrganigrammeInput input) {
        return organigrammePort.creerVersion(input);
    }
}
