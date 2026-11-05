package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.outputs.ObjectifUniteOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListerObjectifsUniteUseCase {

    private final OrganigrammePort organigrammePort;

    public List<ObjectifUniteOutput> execute(UUID uniteId, Integer anneeExercice) {
        return organigrammePort.listerObjectifsUnite(uniteId, anneeExercice);
    }
}
