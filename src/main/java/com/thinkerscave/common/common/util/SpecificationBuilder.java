package com.thinkerscave.common.common.util;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lightweight fluent builder for composing {@link Specification} predicates
 * for list/search endpoints.
 *
 * <p>Skips null / blank values automatically so callers can chain optional
 * filters without per-field guards. Use the nested {@link Path} helper for
 * nested attribute access (e.g. {@code "organization.code"}).
 *
 * <p>Example:
 * <pre>{@code
 * Specification<Student> spec = SpecificationBuilder.<Student>builder()
 *     .equal("status", filter.status())
 *     .like("firstName", filter.search())
 *     .equal("organizationId", currentOrgId)
 *     .between("createdAt", filter.from(), filter.to())
 *     .build();
 * }</pre>
 */
public final class SpecificationBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();

    public static <T> SpecificationBuilder<T> builder() {
        return new SpecificationBuilder<>();
    }

    /** Equality filter — skipped if {@code value} is null. */
    public SpecificationBuilder<T> equal(String attribute, Object value) {
        if (value == null) return this;
        if (value instanceof String s && s.isBlank()) return this;
        specs.add((root, q, cb) -> cb.equal(resolve(root, attribute), value));
        return this;
    }

    /** Case-insensitive {@code LIKE %value%} filter — skipped if blank. */
    public SpecificationBuilder<T> like(String attribute, String value) {
        if (value == null || value.isBlank()) return this;
        String pattern = "%" + value.trim().toLowerCase() + "%";
        specs.add((root, q, cb) -> cb.like(cb.lower(resolve(root, attribute).as(String.class)), pattern));
        return this;
    }

    /** Multi-attribute case-insensitive search (OR across attributes). */
    public SpecificationBuilder<T> searchAcross(String value, String... attributes) {
        if (value == null || value.isBlank() || attributes.length == 0) return this;
        String pattern = "%" + value.trim().toLowerCase() + "%";
        specs.add((root, q, cb) -> {
            Predicate[] preds = new Predicate[attributes.length];
            for (int i = 0; i < attributes.length; i++) {
                preds[i] = cb.like(cb.lower(resolve(root, attributes[i]).as(String.class)), pattern);
            }
            return cb.or(preds);
        });
        return this;
    }

    /** {@code IN (...)} filter — skipped if collection is null/empty. */
    public SpecificationBuilder<T> in(String attribute, Collection<?> values) {
        if (values == null || values.isEmpty()) return this;
        specs.add((root, q, cb) -> resolve(root, attribute).in(values));
        return this;
    }

    /** Inclusive date range — either bound may be null. */
    public SpecificationBuilder<T> between(String attribute, LocalDate from, LocalDate to) {
        if (from == null && to == null) return this;
        specs.add((root, q, cb) -> {
            Path<LocalDate> path = resolve(root, attribute);
            if (from != null && to != null) return cb.between(path, from, to);
            if (from != null) return cb.greaterThanOrEqualTo(path, from);
            return cb.lessThanOrEqualTo(path, to);
        });
        return this;
    }

    /** Boolean flag filter. Pass {@code null} to skip. */
    public SpecificationBuilder<T> isTrue(String attribute, Boolean value) {
        if (value == null) return this;
        specs.add((root, q, cb) -> cb.equal(resolve(root, attribute), value));
        return this;
    }

    public Specification<T> build() {
        return specs.stream().reduce(Specification::and).orElse((r, q, cb) -> cb.conjunction());
    }

    @SuppressWarnings("unchecked")
    private static <X> Path<X> resolve(jakarta.persistence.criteria.Root<?> root, String attribute) {
        if (!attribute.contains(".")) return (Path<X>) root.get(attribute);
        Path<?> path = root;
        for (String segment : attribute.split("\\.")) {
            path = path.get(segment);
        }
        return (Path<X>) path;
    }
}
