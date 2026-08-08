package com.thinkerscave.communication.service.impl;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.communication.dto.request.MessageRequest;
import com.thinkerscave.communication.dto.request.MessageThreadRequest;
import com.thinkerscave.communication.dto.response.MessageResponse;
import com.thinkerscave.communication.dto.response.MessageThreadResponse;
import com.thinkerscave.communication.entity.Message;
import com.thinkerscave.communication.entity.MessageThread;
import com.thinkerscave.communication.repository.MessageRepository;
import com.thinkerscave.communication.repository.MessageThreadRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessageServiceImpl authorization")
class MessageServiceImplTest {

    private static final Long ORG_ID = 7L;
    private static final Long USER_1 = 1L;
    private static final Long USER_21 = 21L;

    @Mock
    private MessageThreadRepository threadRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    @BeforeEach
    void setUp() {
        OrganizationContext.setOrganizationId(ORG_ID);
        authenticateAs("admin.user", USER_1);
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getMyThreads queries with authenticated user id only (not client-supplied)")
    void getMyThreadsUsesSecurityContextUserId() {
        Pageable pageable = PageRequest.of(0, 20);
        when(threadRepository.findActiveThreadsForUser(eq(ORG_ID), eq("1"), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        messageService.getMyThreads(pageable);

        verify(threadRepository).findActiveThreadsForUser(ORG_ID, "1", pageable);
        verify(threadRepository, never()).findActiveThreadsForUser(eq(ORG_ID), eq("21"), any());
    }

    @Test
    @DisplayName("createThread always includes authenticated user as participant")
    void createThreadIncludesCurrentUser() {
        MessageThreadRequest request = new MessageThreadRequest();
        request.setSubject("Hello");
        request.setParticipantUserIds(List.of(USER_21));

        when(threadRepository.save(any(MessageThread.class))).thenAnswer(inv -> {
            MessageThread t = inv.getArgument(0);
            t.setThreadId(99L);
            return t;
        });

        MessageThreadResponse response = messageService.createThread(request);

        ArgumentCaptor<MessageThread> captor = ArgumentCaptor.forClass(MessageThread.class);
        verify(threadRepository).save(captor.capture());
        String csv = captor.getValue().getParticipantUserIdsCsv();
        assertTrue(csv.contains("1"), "current user must be participant");
        assertTrue(csv.contains("21"), "requested participant retained");
        assertEquals(99L, response.getThreadId());
    }

    @Test
    @DisplayName("sendMessage rejects non-participant (IDOR)")
    void sendMessageRejectsNonParticipant() {
        MessageThread thread = thread(10L, "21,100", false);
        when(threadRepository.findByThreadIdAndOrganizationId(10L, ORG_ID)).thenReturn(Optional.of(thread));

        MessageRequest request = new MessageRequest();
        request.setBody("secret");

        assertThrows(AccessDeniedException.class,
                () -> messageService.sendMessage(10L, request));
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("sendMessage allows exact participant and stamps sender from security context")
    void sendMessageAsParticipant() {
        MessageThread thread = thread(10L, "1,21", false);
        when(threadRepository.findByThreadIdAndOrganizationId(10L, ORG_ID)).thenReturn(Optional.of(thread));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setMessageId(5L);
            return m;
        });
        when(threadRepository.save(any(MessageThread.class))).thenAnswer(inv -> inv.getArgument(0));

        MessageRequest request = new MessageRequest();
        request.setBody("hi");

        MessageResponse response = messageService.sendMessage(10L, request);

        assertEquals(USER_1, response.getSenderUserId());
        assertEquals("hi", response.getBody());
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertEquals(USER_1, captor.getValue().getSenderUserId());
    }

    @Test
    @DisplayName("sendMessage blocks closed threads")
    void sendMessageBlocksClosedThread() {
        MessageThread thread = thread(10L, "1,21", true);
        when(threadRepository.findByThreadIdAndOrganizationId(10L, ORG_ID)).thenReturn(Optional.of(thread));

        MessageRequest request = new MessageRequest();
        request.setBody("late");

        assertThrows(BadRequestException.class,
                () -> messageService.sendMessage(10L, request));
    }

    @Test
    @DisplayName("getMessages rejects non-participant even with valid threadId")
    void getMessagesRejectsNonParticipant() {
        MessageThread thread = thread(10L, "21", false);
        when(threadRepository.findByThreadIdAndOrganizationId(10L, ORG_ID)).thenReturn(Optional.of(thread));

        assertThrows(AccessDeniedException.class,
                () -> messageService.getMessages(10L, PageRequest.of(0, 50)));
        verify(messageRepository, never())
                .findByMessageThreadIdAndDeletedFalseOrderBySentAtAsc(anyLong(), any());
    }

    @Test
    @DisplayName("getMessages returns page for participant")
    void getMessagesAllowsParticipant() {
        MessageThread thread = thread(10L, "1,21", false);
        when(threadRepository.findByThreadIdAndOrganizationId(10L, ORG_ID)).thenReturn(Optional.of(thread));
        when(messageRepository.findByMessageThreadIdAndDeletedFalseOrderBySentAtAsc(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of()));

        Page<MessageResponse> page = messageService.getMessages(10L, PageRequest.of(0, 50));
        assertNotNull(page);
        verify(messageRepository).findByMessageThreadIdAndDeletedFalseOrderBySentAtAsc(eq(10L), any());
    }

    @Test
    @DisplayName("substring trap: participant CSV '21' does not authorize user 1")
    void substringTrapDoesNotAuthorize() {
        MessageThread thread = thread(10L, "21", false);
        when(threadRepository.findByThreadIdAndOrganizationId(10L, ORG_ID)).thenReturn(Optional.of(thread));

        assertThrows(AccessDeniedException.class,
                () -> messageService.getMessages(10L, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("unauthenticated caller is denied")
    void unauthenticatedDenied() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class,
                () -> messageService.getMyThreads(PageRequest.of(0, 10)));
    }

    private void authenticateAs(String username, Long userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "n/a", List.of()));
    }

    private static MessageThread thread(Long id, String csv, boolean closed) {
        MessageThread t = new MessageThread();
        t.setThreadId(id);
        t.setOrganizationId(ORG_ID);
        t.setParticipantUserIdsCsv(csv);
        t.setClosed(closed);
        t.setSubject("t");
        return t;
    }
}
