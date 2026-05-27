package com.thinkerscave.common.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;

/**
 * Builds {@link Pageable} instances from query-string style inputs in a
 * tolerant, defensible way. Page is 0-based at the HTTP boundary;
 * negative/zero values are normalised; size is capped to {@link #MAX_SIZE}.
 */
public final class PageRequestUtil {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    private PageRequestUtil() { }

    public static Pageable of(Integer page, Integer size, String sort) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(p, s, parseSort(sort));
    }

    /**
     * Accepts {@code "field"} or {@code "field,direction"} (comma-separated).
     * Multiple orderings may be passed as {@code "f1,asc;f2,desc"}.
     */
    public static Sort parseSort(String sortExpr) {
        if (sortExpr == null || sortExpr.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        var orders = Arrays.stream(sortExpr.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(token -> {
                    String[] parts = token.split(",");
                    String field = parts[0].trim();
                    Sort.Direction dir = (parts.length > 1
                            && "desc".equalsIgnoreCase(parts[1].trim()))
                            ? Sort.Direction.DESC
                            : Sort.Direction.ASC;
                    return new Sort.Order(dir, field);
                })
                .toList();
        return orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    }
}
