package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.shared.dto.PageResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectOrderingTest {

    @Test
    void ordersTeachingSubjectsThenLabsThenActivitiesEachAlphabetically() {
        List<Subject> subjects = new ArrayList<>(List.of(
                subject(1L, "Yoga", SubjectType.ACTIVITY),
                subject(2L, "Zoology", SubjectType.ACADEMIC_SUBJECT),
                subject(3L, "Physics Lab", SubjectType.PRACTICAL_LAB),
                subject(4L, "Algebra", SubjectType.ACADEMIC_SUBJECT),
                subject(5L, "Art Club", SubjectType.ACTIVITY),
                subject(6L, "Chemistry Lab", SubjectType.PRACTICAL_LAB)));

        subjects.sort(SubjectOrdering.SUBJECTS);

        assertEquals(List.of("Algebra", "Zoology", "Chemistry Lab", "Physics Lab", "Art Club", "Yoga"),
                subjects.stream().map(Subject::getName).toList());
    }

    @Test
    void pagesSortedSubjectsWithoutLosingTheGlobalTypeOrder() {
        List<Subject> subjects = new ArrayList<>(List.of(
                subject(1L, "Yoga", SubjectType.ACTIVITY),
                subject(2L, "Maths", SubjectType.ACADEMIC_SUBJECT),
                subject(3L, "Physics Lab", SubjectType.PRACTICAL_LAB),
                subject(4L, "English", SubjectType.ACADEMIC_SUBJECT)));
        subjects.sort(SubjectOrdering.SUBJECTS);

        PageResponse<Subject> page = SubjectOrdering.page(
                SubjectOrdering.slice(subjects, 1, 2), subjects.size(), 1, 2, subject -> subject);

        assertEquals(List.of("Physics Lab", "Yoga"), page.getContent().stream().map(Subject::getName).toList());
        assertEquals(4, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getPage());
        assertEquals(2, page.getSize());
        assertFalse(page.isFirst());
        assertTrue(page.isLast());
    }

    private static Subject subject(long id, String name, SubjectType type) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName(name);
        subject.setSubjectType(type);
        subject.setActive(true);
        return subject;
    }
}
