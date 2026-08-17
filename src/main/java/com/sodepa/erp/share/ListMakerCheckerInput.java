package com.sodepa.erp.share;

import com.sodepa.erp.utils.MakerCheckerStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.data.domain.Pageable;

@Builder
public record ListMakerCheckerInput(
        @NotNull MakerCheckerStatus status,
        @NotNull Pageable pageable

) {
}
