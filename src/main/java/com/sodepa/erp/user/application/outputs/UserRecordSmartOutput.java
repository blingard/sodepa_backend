package com.sodepa.erp.user.application.outputs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserRecordSmartOutput(
        @NotNull UUID id,
        @NotBlank String name,
        @NotBlank String image,
        boolean status
) {
}
