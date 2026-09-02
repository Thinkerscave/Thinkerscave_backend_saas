package com.thinkerscave.shared.pagination;

/**
 * Canonical pagination defaults for list APIs. Controllers should use
 * {@link org.springframework.data.web.PageableDefault} with {@link #DEFAULT_SIZE}
 * or {@link com.thinkerscave.shared.util.PageRequestUtil}.
 */
public final class PaginationConstants {

    public static final int DEFAULT_SIZE = 10;
    public static final int DEFAULT_GRID_SIZE = 12;
    public static final int MAX_SIZE = 100;

    private PaginationConstants() {}
}
