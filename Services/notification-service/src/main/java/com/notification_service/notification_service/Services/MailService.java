package com.notification_service.notification_service.Services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends a plain text email to the specified recipient.
     */
    public void sendEmail(String toEmail, String subject, String body) {
        sendEmail(toEmail, subject, body, false);
    }

    /**
     * Sends an HTML email to the specified recipient.
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        sendEmail(toEmail, subject, htmlBody, true);
    }

    /**
     * Internal method to send email with HTML support.
     */
    private void sendEmail(String toEmail, String subject, String body, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, isHtml); // Second parameter enables HTML

            mailSender.send(message);
            log.info("Successfully sent {} email to {} with subject: {}",
                    isHtml ? "HTML" : "plain text", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
            // Depending on policy, you might want to retry sending here.
        }
    }
}