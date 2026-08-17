package com.sodepa.erp.utils;

import lombok.Builder;
import org.springframework.data.domain.Sort;

import java.util.List;

@Builder
public record PageRecord<T>(
        List<T> content,
        boolean empty,
        boolean first,
        boolean last,
        int number,
        int numberOfElements,
        PageableRecord pageable,
        int size,
        Sort sort,
        long totalElements,
        int totalPages
) {
}
