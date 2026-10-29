package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.shared.dto.PageResponse;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Canonical display order for every subject list and dropdown.
 *
 * <p>The order follows the declaration order of {@link SubjectType} (teaching subjects,
 * practical labs, activities) and sorts alphabetically inside each type. Deriving the rank
 * from the enum means a new subject type only has to be declared once; no screen keeps its
 * own hard-coded type list.
 */
final class SubjectOrdering {

    /** Subject comparator: type rank, then case-insensitive name, then id for a stable order. */
    static final Comparator<Subject> SUBJECTS = Comparator
            .comparing(Subject::getSubjectType, types())
            .thenComparing(Subject::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            .thenComparing(Subject::getSubjectId, Comparator.nullsLast(Comparator.naturalOrder()));

    private SubjectOrdering() {
    }

    /** Rank of a subject type, taken from the enum declaration order. */
    static int rank(SubjectType type) {
        return type == null ? Integer.MAX_VALUE : type.ordinal();
    }

    static Comparator<SubjectType> types() {
        return Comparator.comparingInt(SubjectOrdering::rank);
    }

    /** Comparator for any row that exposes a subject type and a display name. */
    static <T> Comparator<T> byType(Function<T, SubjectType> type, Function<T, String> name) {
        return Comparator.comparing(type, types())
                .thenComparing(name, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
    }

    /** Page slice of an already sorted list. */
    static <E> List<E> slice(List<E> rows, int page, int size) {
        int from = Math.min(rows.size(), Math.max(0, page) * size);
        int to = Math.min(rows.size(), from + size);
        return rows.subList(from, to);
    }

    /**
     * Builds a paged response for a list sorted by {@link #SUBJECTS}. Subject type order is an
     * enum concern, not a database column, so the slice is taken after sorting in memory.
     */
    static <E, T> PageResponse<T> page(List<E> pageRows, int totalElements, int page, int size,
                                       Function<E, T> mapper) {
        int safePage = Math.max(0, page);
        int totalPages = size <= 0 ? 0 : (int) Math.ceil(totalElements / (double) size);
        return PageResponse.<T>builder()
                .content(pageRows.stream().map(mapper).toList())
                .page(safePage)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(safePage == 0)
                .last(totalPages == 0 || safePage >= totalPages - 1)
                .build();
    }
}
