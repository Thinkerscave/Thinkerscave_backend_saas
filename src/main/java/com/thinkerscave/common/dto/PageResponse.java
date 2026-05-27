package com.thinkerscave.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Standardized paged response wrapper used for every list endpoint.
 *
 * <p>Usage:
 * <pre>{@code
 * Page<Student> page = studentRepository.findAll(spec, pageable);
 * PageResponse<StudentResponseDTO> dto = PageResponse.of(page, studentMapper::toResponse);
 * return ResponseEntity.ok(ApiResponse.success(dto));
 * }</pre>
 *
 * @param <T> element type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private String sort;

    /**
     * Build a PageResponse from a Spring Data {@link Page} with an element mapper.
     */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return PageResponse.<T>builder()
                .content(page.getContent().stream().map(mapper).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .sort(page.getSort().isSorted() ? page.getSort().toString() : null)
                .build();
    }

    /**
     * Build a PageResponse directly (when content is already of the target type).
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page, Function.identity());
    }
}
