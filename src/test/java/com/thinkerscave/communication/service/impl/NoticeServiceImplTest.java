package com.thinkerscave.communication.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRoleRepository;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.communication.dto.response.NoticeResponse;
import com.thinkerscave.communication.entity.Notice;
import com.thinkerscave.communication.entity.NoticeAudience;
import com.thinkerscave.communication.enums.NoticeAudienceType;
import com.thinkerscave.communication.enums.NoticeStatus;
import com.thinkerscave.communication.repository.NoticeAudienceRepository;
import com.thinkerscave.communication.repository.NoticeRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeServiceImpl publish audit")
class NoticeServiceImplTest {

    private static final Long ORG_ID = 7L;
    private static final Long USER_ID = 42L;

    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NoticeAudienceRepository audienceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TeacherAllocationTeacherRepository allocationTeacherRepository;

    @InjectMocks
    private NoticeServiceImpl noticeService;

    @BeforeEach
    void setUp() {
        OrganizationContext.setOrganizationId(ORG_ID);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("publisher.user", "n/a", List.of()));
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("publish sets publishedByUserId from authenticated user")
    void publishSetsPublishedByUserId() {
        Notice notice = new Notice();
        notice.setNoticeId(9L);
        notice.setOrganizationId(ORG_ID);
        notice.setTitle("Exam");
        notice.setStatus(NoticeStatus.DRAFT);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("publisher.user");

        when(noticeRepository.findByNoticeIdAndOrganizationId(9L, ORG_ID)).thenReturn(Optional.of(notice));
        when(userRepository.findByUsername("publisher.user")).thenReturn(Optional.of(user));
        when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(audienceRepository.findByNoticeId(9L)).thenReturn(List.of());

        NoticeResponse response = noticeService.publish(9L);

        ArgumentCaptor<Notice> captor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository).save(captor.capture());
        assertEquals(USER_ID, captor.getValue().getPublishedByUserId());
        assertEquals(NoticeStatus.PUBLISHED, captor.getValue().getStatus());
        assertEquals(USER_ID, response.getPublishedByUserId());
        assertNotNull(captor.getValue().getPublishDate());
    }

    @Test
    @DisplayName("recent staff notices respect audience and expiry")
    void recentPublishedNoticesRespectStaffAudienceAndExpiry() {
        Notice staffNotice = notice(11L, "Staff briefing", null);
        Notice studentNotice = notice(12L, "Student assembly", null);
        Notice expiredNotice = notice(13L, "Expired staff note", LocalDate.now().minusDays(1));
        NoticeAudience staffAudience = audience(11L, NoticeAudienceType.STAFF);
        NoticeAudience studentsAudience = audience(12L, NoticeAudienceType.STUDENTS);
        NoticeAudience expiredAudience = audience(13L, NoticeAudienceType.STAFF);

        when(noticeRepository.findByOrganizationIdAndStatusOrderByPublishDateDesc(
                eq(ORG_ID), eq(NoticeStatus.PUBLISHED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(staffNotice, studentNotice, expiredNotice), PageRequest.of(0, 50), 3));
        when(audienceRepository.findByNoticeIdIn(List.of(11L, 12L)))
            .thenReturn(List.of(staffAudience, studentsAudience));
        when(userRoleRepository.findActiveRolesWithDetails(USER_ID)).thenReturn(List.of());
        when(academicYearRepository.findByCurrentYearTrue()).thenReturn(Optional.empty());

        List<NoticeResponse> results = noticeService.getRecentPublishedForStaff(USER_ID, 99L, 4);

        assertEquals(List.of("Staff briefing"), results.stream().map(NoticeResponse::getTitle).toList());
        verify(audienceRepository, never()).findByNoticeId(12L);
        verify(audienceRepository, never()).findByNoticeId(13L);
    }

    private Notice notice(Long id, String title, LocalDate expiryDate) {
        Notice notice = new Notice();
        notice.setNoticeId(id);
        notice.setOrganizationId(ORG_ID);
        notice.setTitle(title);
        notice.setStatus(NoticeStatus.PUBLISHED);
        notice.setPublishDate(LocalDate.now());
        notice.setExpiryDate(expiryDate);
        return notice;
    }

    private NoticeAudience audience(Long noticeId, NoticeAudienceType type) {
        NoticeAudience audience = new NoticeAudience();
        audience.setNoticeId(noticeId);
        audience.setAudienceType(type);
        return audience;
    }
}
