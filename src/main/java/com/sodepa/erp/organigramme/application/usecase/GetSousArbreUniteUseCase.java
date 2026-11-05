package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.outputs.ArborescenceNoeudOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetSousArbreUniteUseCase implements UseCase<UUID, ArborescenceNoeudOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public ArborescenceNoeudOutput execute(UUID uniteId) {
        return organigrammePort.getSousArbreUnite(uniteId);
    }
}
