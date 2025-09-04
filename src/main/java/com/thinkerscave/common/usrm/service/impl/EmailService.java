package com.thinkerscave.common.usrm.service.impl;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
//    @Autowired
//    private JavaMailSender mailSender;
//
//    public void sendSimpleMessage(String to, String subject, String text) {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setFrom("");
//        message.setTo(to);
//        message.setSubject(subject);
//        message.setText(text);
//        mailSender.send(message);
//    }
@Autowired
private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail; // Inject the 'from' address from properties

    /**
     * Sends a rich HTML email. This is the preferred method.
     * The method is asynchronous to avoid blocking the main application thread.
     *
     * @param to      The recipient's email address.
     * @param subject The email subject.
     * @param htmlBody The HTML content of the email.
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // Set the second parameter to 'true' to send HTML

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            // It's crucial to log errors in an async method
            System.err.println("Failed to send HTML email to " + to + ": " + e.getMessage());
        }
    }
}

