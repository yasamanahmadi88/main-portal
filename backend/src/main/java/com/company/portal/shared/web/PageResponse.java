package com.company.portal.shared.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Shape of paged responses matching the OpenAPI {@code PageResponse} contract.
 * Feature modules return this instead of Spring Data's {@link Page} so the
 * JSON shape is stable and contract-driven.
 */
public record PageResponse<T>(
        int page,
        int size,
        long totalElements,
        int totalPages,
        int numberOfElements,
        boolean first,
        boolean last,
        List<T> content
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumberOfElements(),
                page.isFirst(),
                page.isLast(),
                page.getContent()
        );
    }

    public <R> PageResponse<R> map(java.util.function.Function<T, R> mapper) {
        return new PageResponse<>(page, size, totalElements, totalPages, numberOfElements,
                first, last, content.stream().map(mapper).toList());
    }
}
