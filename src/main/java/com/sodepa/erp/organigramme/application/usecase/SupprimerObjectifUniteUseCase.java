package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `SupprimerObjectifUniteUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class SupprimerObjectifUniteUseCase {

    private final OrganigrammePort organigrammePort;

    public void execute(UUID id) {
        organigrammePort.supprimerObjectif(id);
    }
}
