package com.notification_service.notification_service.Services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_service.notification_service.Dto.StorageUpgradeNotification;
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
public class StorageUpgradeConsumer {

    private final ObjectMapper objectMapper;
    private final MailService mailService;

    @Value("classpath:templates/storage-upgrade.html")
    private Resource storageUpgradeTemplate;

    private String htmlTemplate;

    @PostConstruct
    void loadTemplate() throws Exception {
        htmlTemplate = StreamUtils.copyToString(
                storageUpgradeTemplate.getInputStream(),
                StandardCharsets.UTF_8
        );
    }

    @KafkaListener(
            topics = "storage-upgrade-topic",
            groupId = "notification-service-group"
    )
    public void consumeStorageUpgrade(String message) {

        try {
            StorageUpgradeNotification n =
                    objectMapper.readValue(message, StorageUpgradeNotification.class);

            String subject =
                    "🚀 Success! Your CloudSync Plan is Now the " + n.getNewPlan() + " Plan";

            String body = buildEmailBody(n);

            mailService.sendHtmlEmail(n.getEmail(), subject, body);

            log.info("Storage upgrade email sent to {}", n.getEmail());

        } catch (Exception e) {
            log.error("Failed to process storage upgrade message", e);
        }
    }

    private String buildEmailBody(StorageUpgradeNotification n) {

        String displayName =
                (n.getUsername() != null && !n.getUsername().isBlank())
                        ? n.getUsername()
                        : n.getEmail();

        String formattedDate =
                new SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                        .format(new Date());

        return applyTemplate(htmlTemplate, Map.of(
                "displayName", displayName,
                "newPlan", n.getNewPlan(),
                "newStorageGB", n.getNewStorageGB(),
                "upgradeDate", formattedDate,
                "maxFileSize", getMaxFileSize(n.getNewPlan()),
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

    private String getMaxFileSize(String plan) {
        if (plan == null) return "5 GB";

        return switch (plan.toLowerCase()) {
            case "basic" -> "10 GB";
            case "pro" -> "100 GB";
            case "team" -> "1 TB";
            default -> "5 GB";
        };
    }
}
