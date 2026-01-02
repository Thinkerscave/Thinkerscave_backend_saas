package com.thinkerscave.common.course.service;

import com.thinkerscave.common.course.domain.Syllabus;
import com.thinkerscave.common.course.dto.SyllabusRequestDTO;
import com.thinkerscave.common.course.dto.SyllabusResponseDTO;
import com.thinkerscave.common.course.enums.SyllabusStatus;
import com.thinkerscave.common.course.repository.SyllabusRepository;
import com.thinkerscave.common.course.service.impl.SyllabusServiceImpl;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.course.domain.Subject;
import com.thinkerscave.common.course.repository.SubjectRepository;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SyllabusServiceTest {

    @Mock
    private SyllabusRepository syllabusRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SyllabusServiceImpl syllabusService;

    private Organisation organisation;
    private Subject subject;
    private User user;
    private Syllabus syllabus;

    @BeforeEach
    void setUp() {
        organisation = new Organisation();
        organisation.setOrgId(1L);

        subject = new Subject();
        subject.setSubjectId(1L);
        subject.setOrganization(organisation);

        user = new User();
        user.setId(1L);

        syllabus = new Syllabus();
        syllabus.setSyllabusId(1L);
        syllabus.setSubject(subject);
        syllabus.setStatus(SyllabusStatus.DRAFT);
        syllabus.setVersion("1.0");
    }

    @Test
    void testCreateSyllabus() {
        SyllabusRequestDTO dto = new SyllabusRequestDTO();
        dto.setSubjectId(1L);
        dto.setVersion("1.0");

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(syllabusRepository.save(any(Syllabus.class))).thenReturn(syllabus);
        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(syllabus));

        SyllabusResponseDTO response = syllabusService.createSyllabus(dto);

        assertNotNull(response);
        assertEquals("1.0", response.getVersion());
        verify(syllabusRepository, times(1)).save(any(Syllabus.class));
    }

    @Test
    void testGetSyllabus() {
        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(syllabus));

        SyllabusResponseDTO response = syllabusService.getSyllabus(1L);

        assertNotNull(response);
        assertEquals(1L, response.getSyllabusId());
        verify(syllabusRepository, times(1)).findById(1L);
    }

    @Test
    void testApproveSyllabus() {
        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(syllabus));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        syllabusService.approveSyllabus(1L, 1L);

        assertEquals(SyllabusStatus.APPROVED, syllabus.getStatus());
        assertNotNull(syllabus.getApprovedBy());
        verify(syllabusRepository, times(1)).save(syllabus);
    }

    @Test
    void testPublishSyllabus() {
        syllabus.setStatus(SyllabusStatus.APPROVED);
        when(syllabusRepository.findById(1L)).thenReturn(Optional.of(syllabus));

        syllabusService.publishSyllabus(1L);

        assertEquals(SyllabusStatus.PUBLISHED, syllabus.getStatus());
        verify(syllabusRepository, times(1)).save(syllabus);
    }
}
