package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CreerDelegationInput;
import com.sodepa.erp.organigramme.application.outputs.DelegationOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreerDelegationPouvoirUseCase implements UseCase<CreerDelegationInput, DelegationOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public DelegationOutput execute(CreerDelegationInput input) {
        return organigrammePort.creerDelegation(input);
    }
}
