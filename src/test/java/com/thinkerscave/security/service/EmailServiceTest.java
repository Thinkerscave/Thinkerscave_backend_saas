package com.thinkerscave.security.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.gmail.com");
        ReflectionTestUtils.setField(emailService, "mailPort", 587);
        ReflectionTestUtils.setField(emailService, "mailUsername", "sender@example.com");
        ReflectionTestUtils.setField(emailService, "mailPassword", "app-password");
        ReflectionTestUtils.setField(emailService, "smtpAuth", true);
        ReflectionTestUtils.setField(emailService, "fromEmail", "ThinkersCave <sender@example.com>");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        ReflectionTestUtils.setField(emailService, "redirectTo", "");
        ReflectionTestUtils.setField(emailService, "maxSendAttempts", 3);
        ReflectionTestUtils.setField(emailService, "retryDelayMs", 0L);
        lenient().when(mailSender.createMimeMessage())
                .thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void sendsEmailWhenSmtpIsConfigured() {
        assertTrue(emailService.sendHtmlEmailSync("student@example.com", "Welcome", "<p>Hello</p>"));

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void retriesTransientFailuresAndReportsFailure() {
        doThrow(new MailSendException("temporary outage")).when(mailSender).send(any(MimeMessage.class));

        assertFalse(emailService.sendHtmlEmailSync("student@example.com", "Welcome", "<p>Hello</p>"));

        verify(mailSender, times(3)).send(any(MimeMessage.class));
    }

    @Test
    void doesNotAttemptSendWhenHostIsLocalhost() {
        ReflectionTestUtils.setField(emailService, "mailHost", "localhost");

        assertFalse(emailService.sendHtmlEmailSync("student@example.com", "Welcome", "<p>Hello</p>"));

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void redirectsToConfiguredInbox() throws Exception {
        ReflectionTestUtils.setField(emailService, "redirectTo", "qa-inbox@example.com");

        assertTrue(emailService.sendHtmlEmailSync("student@example.com", "Welcome", "<p>Hello</p>"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("qa-inbox@example.com", captor.getValue().getAllRecipients()[0].toString());
        assertTrue(captor.getValue().getSubject().startsWith("[To: student@example.com]"));
    }

    @Test
    void everyProfileRedirectsToQaInbox() throws Exception {
        // Read the shipped sources of truth, not the test classpath: src/test/resources
        // shadows src/main/resources for application.properties and application-test.properties.
        String[] profiles = {
                "application.properties",
                "application-dev.properties",
                "application-test.properties",
                "application-prod.properties"
        };
        for (String profile : profiles) {
            Path file = Path.of("src", "main", "resources", profile);
            assertTrue(Files.exists(file), file + " should exist");
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            }
            assertEquals("soumyaranjanmahapatra2k@gmail.com",
                    properties.getProperty("app.mail.redirect-to"), profile);
        }
    }
}
