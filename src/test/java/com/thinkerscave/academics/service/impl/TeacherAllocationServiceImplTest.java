package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.TeacherAllocationRowResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeacherAllocationServiceImplTest {

    @Test
    void clampsRequestedPageToLastAvailablePage() {
        List<TeacherAllocationRowResponse> rows = rows(25);

        TeacherAllocationServiceImpl.AllocationPage result =
                TeacherAllocationServiceImpl.paginateRows(rows, 99, 10);

        assertEquals(2, result.page());
        assertEquals(10, result.size());
        assertEquals(3, result.totalPages());
        assertEquals(25, result.totalRows());
        assertEquals(List.of("Subject 20", "Subject 21", "Subject 22", "Subject 23", "Subject 24"),
                result.rows().stream().map(TeacherAllocationRowResponse::getSubjectName).toList());
    }

    @Test
    void boundsPageSizeAndHandlesEmptyResult() {
        TeacherAllocationServiceImpl.AllocationPage result =
                TeacherAllocationServiceImpl.paginateRows(List.of(), -2, 500);

        assertEquals(0, result.page());
        assertEquals(100, result.size());
        assertEquals(0, result.totalPages());
        assertEquals(0, result.totalRows());
        assertEquals(List.of(), result.rows());
    }

    private List<TeacherAllocationRowResponse> rows(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> TeacherAllocationRowResponse.builder()
                        .subjectName("Subject " + index)
                        .build())
                .toList();
    }
}