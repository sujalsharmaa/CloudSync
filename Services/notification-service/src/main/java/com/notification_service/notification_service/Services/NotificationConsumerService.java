package com.notification_service.notification_service.Services;

import com.notification_service.notification_service.Dto.BanNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumerService {

    private final ObjectMapper objectMapper;
    private final MailService mailService;

    @Value("classpath:templates/ban-notification.html")
    private Resource banEmailTemplate;

    private String htmlTemplate;

    private static final String NOTIFICATION_TOPIC = "notification-topic";
    private static final String GROUP_ID = "rag-pipeline-group";

    @PostConstruct
    void loadTemplate() throws Exception {
        htmlTemplate = StreamUtils.copyToString(
                banEmailTemplate.getInputStream(),
                StandardCharsets.UTF_8
        );
    }

    @KafkaListener(topics = NOTIFICATION_TOPIC, groupId = GROUP_ID)
    public void consumeBanNotification(String message) throws Exception {

        BanNotification notification =
                objectMapper.readValue(message, BanNotification.class);

        String subject =
                "URGENT: Account Suspension - " + notification.getBanDuration();

        String htmlBody = buildBanEmailBody(notification);

        mailService.sendHtmlEmail(notification.getEmail(), subject, htmlBody);

        log.info("Ban email sent to {}", notification.getEmail());
    }

    private String buildBanEmailBody(BanNotification n) {

        String displayName =
                (n.getUsername() != null && !n.getUsername().isBlank())
                        ? n.getUsername()
                        : n.getEmail();

        String banDuration =
                n.getBanDuration() != null ? n.getBanDuration() : "Indefinite";

        String banReason =
                n.getBanReason() != null ? n.getBanReason() : "Policy violation";

        boolean isLifetime =
                banDuration.toLowerCase().contains("lifetime");

        String headlineColor =
                isLifetime ? "#ef4444" : "#f59e0b";

        String actionText =
                isLifetime
                        ? "Your account access has been permanently revoked."
                        : "Your access will be restored after the suspension period ends.";

        return applyTemplate(htmlTemplate, Map.of(
                "displayName", displayName,
                "banDuration", banDuration,
                "banReason", banReason,
                "headlineColor", headlineColor,
                "actionText", actionText,
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
