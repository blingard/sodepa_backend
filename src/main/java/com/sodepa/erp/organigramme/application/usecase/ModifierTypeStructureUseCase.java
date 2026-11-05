package com.sodepa.erp.organigramme.application.usecase;

import com.sodepa.erp.organigramme.application.inputs.ModifierTypeStructureInput;
import com.sodepa.erp.organigramme.application.outputs.TypeStructureOutput;
import com.sodepa.erp.organigramme.application.ports.OrganigrammePort;
import com.sodepa.erp.utils.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModifierTypeStructureUseCase implements UseCase<ModifierTypeStructureInput, TypeStructureOutput> {

    private final OrganigrammePort organigrammePort;

    @Override
    public TypeStructureOutput execute(ModifierTypeStructureInput input) {
        return organigrammePort.modifierTypeStructure(input);
    }
}
