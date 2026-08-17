package com.sodepa.erp.utils;

import lombok.Builder;
import org.springframework.data.domain.Sort;

@Builder
public record PageableRecord(
        long offset,
        long pageNumber,
        long pageSize,
        boolean paged,
        Sort sort,
        boolean unpaged

) {
}
