package com.thinkerscave.communication.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.communication.dto.response.NoticeResponse;
import com.thinkerscave.communication.entity.Notice;
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
}
