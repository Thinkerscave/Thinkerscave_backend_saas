package com.thinkerscave.common.common.util;

import com.thinkerscave.common.dto.PageResponse;
import org.springframework.data.domain.Page;

import java.util.function.Function;

/**
 * Thin wrapper around {@link PageResponse#of} kept for discoverability and to
 * provide a single import point when controllers want to map paged results.
 *
 * <p>Most callers can use {@link PageResponse#of} directly; this helper exists
 * so module-level code can do {@code PageMapper.map(page, mapper::toResponse)}.
 */
public final class PageMapper {

    private PageMapper() {}

    public static <E, T> PageResponse<T> map(Page<E> page, Function<E, T> mapper) {
        return PageResponse.of(page, mapper);
    }

    public static <T> PageResponse<T> map(Page<T> page) {
        return PageResponse.of(page);
    }
}
