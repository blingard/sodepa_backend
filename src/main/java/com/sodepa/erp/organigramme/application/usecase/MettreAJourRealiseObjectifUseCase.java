package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.MettreAJourRealiseObjectifInput;
import com.sodepa.erp.organigramme.application.outputs.ObjectifUniteOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MettreAJourRealiseObjectifUseCase implements UseCase<MettreAJourRealiseObjectifInput, ObjectifUniteOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public ObjectifUniteOutput execute(MettreAJourRealiseObjectifInput input) {
        return organigrammePort.mettreAJourRealiseObjectif(input);
    }
}
