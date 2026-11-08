package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerEntityName;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

/**
 * Composant applicatif `GetMakerCheckerByEntityPkAndEntityNameInput` du système ERP SODEPA.
 */
@Builder
public record GetMakerCheckerByEntityPkAndEntityNameInput(
        @NotNull MakerCheckerEntityName entityName,
        @NotNull UUID entityPk

) {
}
