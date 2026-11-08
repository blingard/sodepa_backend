package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.outputs.ArborescenceNoeudOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `GetArborescenceCompleteUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class GetArborescenceCompleteUseCase {

    private final OrganigrammePort organigrammePort;

    public List<ArborescenceNoeudOutput> execute() {
        return organigrammePort.getArborescenceComplete();
    }
}
