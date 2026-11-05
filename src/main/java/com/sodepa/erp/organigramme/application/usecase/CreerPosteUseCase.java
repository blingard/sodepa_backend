package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CreerPosteInput;
import com.sodepa.erp.organigramme.application.outputs.PosteOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreerPosteUseCase implements UseCase<CreerPosteInput, PosteOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public PosteOutput execute(CreerPosteInput input) {
        return organigrammePort.creerPoste(input);
    }
}
