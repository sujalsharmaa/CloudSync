package Services;

import Dto.BanNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MailService mailService; // <-- INJECT MAIL SERVICE

    private static final String NOTIFICATION_TOPIC = "notification-topic";
    private static final String GROUP_ID = "rag-pipeline-group-notification";

    /**
     * Consumes messages from the ban notification topic, sends an appropriate email.
     * @param message The JSON string containing the BanNotification data.
     */
    @KafkaListener(topics = NOTIFICATION_TOPIC,groupId = GROUP_ID)
    public void consumeBanNotification(String message) {
        try {
            BanNotification notification = objectMapper.readValue(message, BanNotification.class);
            log.info("Received ban notification for user: {}", notification.getEmail());

            // --- 1. Compose Email Content ---
            String subject = String.format("URGENT: Account Suspension - %s", notification.getBanDuration());
            String body = String.format(
                    "Dear %s,\n\n" +
                            "Your CloudSync account has been suspended for %s (starting immediately) due to repeated violation of our Content Policy.\n\n" +
                            "Violation Details:\n" +
                            "  - Reason: %s\n" +
                            "  - Current Violation Count: Banning threshold reached for %s duration.\n\n" +
                            "Your access to upload new files is revoked during this period.\n\n" +
                            "If you believe this ban is in error, please reply to this email.",
                    notification.getUsername(),
                    notification.getBanDuration(),
                    notification.getBanReason(),
                    notification.getBanDuration()
            );

            // --- 2. Send Email via MailService ---
            mailService.sendEmail(notification.getEmail(), subject, body);

            log.info("Successfully processed and sent ban email to {}.", notification.getEmail());

        } catch (Exception e) {
            log.error("Error processing ban notification message: {}", message, e);
        }
    }
}