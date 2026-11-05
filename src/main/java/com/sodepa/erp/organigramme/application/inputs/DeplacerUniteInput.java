package com.sodepa.erp.organigramme.application.inputs;

import lombok.Builder;

import java.util.UUID;

@Builder
public record DeplacerUniteInput(
        UUID uniteId,
        UUID nouveauParentId
) {}
