package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.CreerObjectifInput;
import com.sodepa.erp.organigramme.application.outputs.ObjectifUniteOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AjouterObjectifUniteUseCase implements UseCase<CreerObjectifInput, ObjectifUniteOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public ObjectifUniteOutput execute(CreerObjectifInput input) {
        return organigrammePort.ajouterObjectif(input);
    }
}
