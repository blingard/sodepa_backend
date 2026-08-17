package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerEntityName;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record GetMakerCheckerByEntityPkAndEntityNameInput(
        @NotNull MakerCheckerEntityName entityName,
        @NotNull UUID entityPk

) {
}
