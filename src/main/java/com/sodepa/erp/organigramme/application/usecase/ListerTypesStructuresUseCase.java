package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.outputs.TypeStructureOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListerTypesStructuresUseCase {

    private final OrganigrammePort organigrammePort;

    public List<TypeStructureOutput> execute() {
        return organigrammePort.listerTypesStructures();
    }
}
