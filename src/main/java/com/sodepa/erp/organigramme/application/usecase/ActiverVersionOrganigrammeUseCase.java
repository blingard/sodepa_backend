package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.outputs.OrganigrammeVersionOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `ActiverVersionOrganigrammeUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class ActiverVersionOrganigrammeUseCase {

    private final OrganigrammePort organigrammePort;

    public OrganigrammeVersionOutput execute(UUID versionId, UUID approuvePar) {
        return organigrammePort.activerVersion(versionId, approuvePar);
    }
}
