package com.notification_service.notification_service.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_service.notification_service.Dto.WelcomeEmailNotification;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WelcomeEmailConsumer {

    private final ObjectMapper objectMapper;
    private final MailService mailService;

    @Value("classpath:templates/welcome-email.html")
    private Resource welcomeEmailTemplate;

    private String htmlTemplate;

    @PostConstruct
    void loadTemplate() throws Exception {
        htmlTemplate = StreamUtils.copyToString(
                welcomeEmailTemplate.getInputStream(),
                StandardCharsets.UTF_8
        );
    }

    @KafkaListener(topics = "welcome-email-topic", groupId = "rag-pipeline-group")
    public void consumeWelcomeEmail(String message) {

        try {
            WelcomeEmailNotification n =
                    objectMapper.readValue(message, WelcomeEmailNotification.class);

            String subject =
                    "Welcome to CloudSync! Your Secure Cloud Drive is Ready 🚀";

            String body = buildEmailBody(n);

            mailService.sendHtmlEmail(n.getEmail(), subject, body);

            log.info("Welcome email sent to {}", n.getEmail());

        } catch (Exception e) {
            log.error("Failed to process welcome email", e);
        }
    }

    private String buildEmailBody(WelcomeEmailNotification n) {

        String displayName =
                (n.getUsername() != null && !n.getUsername().isBlank())
                        ? n.getUsername()
                        : n.getEmail();

        String registrationDate =
                new SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                        .format(new Date());

        return applyTemplate(htmlTemplate, Map.of(
                "displayName", displayName,
                "username", n.getUsername(),
                "email", n.getEmail(),
                "registrationDate", registrationDate,
                "year", Year.now().getValue()
        ));
    }

    private String applyTemplate(String template, Map<String, Object> values) {
        String result = template;
        for (var entry : values.entrySet()) {
            result = result.replace(
                    "{{" + entry.getKey() + "}}",
                    String.valueOf(entry.getValue())
            );
        }
        return result;
    }
}
