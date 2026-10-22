package com.thinkerscave.staff.service;

import com.thinkerscave.academics.entity.Subject;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.StaffType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StaffSpecializationServiceTest {

    private SubjectRepository subjectRepository;
    private StaffSpecializationService service;
    private Staff staff;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        service = new StaffSpecializationService(subjectRepository);
        staff = new Staff();
        staff.setStaffId(7L);
    }

    @Test
    void teachingStaffMustHavePrimarySubject() {
        assertThrows(BadRequestException.class,
                () -> service.assign(staff, StaffType.TEACHING, null, 2L));
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void secondarySubjectIsOptional() {
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L)));

        service.assign(staff, StaffType.TEACHING, 1L, null);

        assertEquals(1L, staff.getPrimarySubject().getSubjectId());
        assertNull(staff.getSecondarySubject());
    }

    @Test
    void sameSubjectCannotFillBothSlots() {
        assertThrows(BadRequestException.class,
                () -> service.assign(staff, StaffType.TEACHING, 1L, 1L));
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void nonTeachingStaffCannotHaveSubjects() {
        assertThrows(BadRequestException.class,
                () -> service.assign(staff, StaffType.NON_TEACHING, 1L, null));
        verifyNoInteractions(subjectRepository);
    }

    @Test
    void nonTeachingStaffClearsSlots() {
        staff.setPrimarySubject(subject(1L));

        service.assign(staff, StaffType.NON_TEACHING, null, null);

        assertNull(staff.getPrimarySubject());
    }

    @Test
    void unknownSubjectIsRejected() {
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L)));

        assertThrows(BadRequestException.class,
                () -> service.assign(staff, StaffType.TEACHING, 1L, 9L));
    }

    @Test
    void inactiveSubjectIsRejected() {
        Subject inactive = subject(2L);
        inactive.setActive(false);
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L), inactive));

        assertThrows(BadRequestException.class,
                () -> service.assign(staff, StaffType.TEACHING, 1L, 2L));
    }

    @Test
    void primaryAndSecondaryAreAssigned() {
        when(subjectRepository.findAllById(any())).thenReturn(List.of(subject(1L), subject(2L)));

        service.assign(staff, StaffType.TEACHING, 1L, 2L);

        assertEquals(1L, staff.getPrimarySubject().getSubjectId());
        assertEquals(2L, staff.getSecondarySubject().getSubjectId());
    }

    private static Subject subject(Long id) {
        Subject subject = new Subject();
        subject.setSubjectId(id);
        subject.setName("Subject " + id);
        subject.setActive(true);
        return subject;
    }
}
