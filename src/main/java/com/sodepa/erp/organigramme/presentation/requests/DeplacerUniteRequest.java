package com.sodepa.erp.organigramme.presentation.requests;

import com.sodepa.erp.organigramme.application.inputs.DeplacerUniteInput;
import java.util.UUID;

public record DeplacerUniteRequest(
        UUID nouveauParentId
) {
    public DeplacerUniteInput toInput(UUID uniteId) {
        return DeplacerUniteInput.builder()
                .uniteId(uniteId)
                .nouveauParentId(nouveauParentId)
                .build();
    }
}
