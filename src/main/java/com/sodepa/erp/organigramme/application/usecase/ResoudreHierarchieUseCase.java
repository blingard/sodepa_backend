package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.outputs.ChaineHierarchiqueOutput;
import com.sodepa.erp.organigramme.application.ports.HierarchieResolutionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service métier d'application implémentant la logique et les règles de gestion pour `ResoudreHierarchieUseCase` au sein de la SODEPA.
 */
@Service
@RequiredArgsConstructor
public class ResoudreHierarchieUseCase {

    private final HierarchieResolutionPort hierarchiePort;

    public ChaineHierarchiqueOutput execute(UUID utilisateurId, int maxNiveaux) {
        return hierarchiePort.getChaineHierarchiqueDetaillee(utilisateurId, maxNiveaux);
    }
}
