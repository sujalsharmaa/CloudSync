package com.notification_service.notification_service.Services;

import com.notification_service.notification_service.Exception.EmailSendingException; // Import custom exception
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

    public void sendEmail(String toEmail, String subject, String body) {
        sendEmail(toEmail, subject, body, false);
    }

    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        sendEmail(toEmail, subject, htmlBody, true);
    }

    private void sendEmail(String toEmail, String subject, String body, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, isHtml);

            mailSender.send(message);
            log.info("Successfully sent {} email to {} with subject: {}",
                    isHtml ? "HTML" : "plain text", toEmail, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            // THROW the exception so the caller (Kafka Consumer) knows it failed
            throw new EmailSendingException("Failed to send email to " + toEmail, e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", toEmail, e.getMessage());
            throw new EmailSendingException("Unexpected error during email sending", e);
        }
    }
}