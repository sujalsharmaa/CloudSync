package com.notification_service.notification_service.Services;

import com.notification_service.notification_service.Dto.BanNotification;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor // Spring uses this constructor to inject MailService
public class NotificationConsumerService {
    private final ObjectMapper objectMapper;
    private final MailService mailService;

    private static final String NOTIFICATION_TOPIC = "notification-topic";
    private static final String GROUP_ID = "rag-pipeline-group";

    /**
     * Consumes messages from the ban notification topic, sends an appropriate email.
     * @param message The JSON string containing the BanNotification data.
     */
    @KafkaListener(topics = NOTIFICATION_TOPIC, groupId = GROUP_ID)
    public void consumeBanNotification(String message) throws Exception {
        // Removed try-catch to allow EmailSendingException to bubble up for Retry
            BanNotification notification = objectMapper.readValue(message, BanNotification.class);
            log.info("Received ban notification for user: {}", notification.getEmail());

            String subject = String.format("URGENT: Account Suspension - %s", notification.getBanDuration());
            String htmlBody = buildBanEmailBody(notification);

            mailService.sendHtmlEmail(notification.getEmail(), subject, htmlBody);

            log.info("Successfully processed and sent ban email to {}.", notification.getEmail());

        // Note: EmailSendingException is NOT caught here, so Kafka will retry.
    }

    private String buildBanEmailBody(BanNotification notification) {
        String displayName = notification.getUsername() != null && !notification.getUsername().isBlank()
                ? notification.getUsername()
                : notification.getEmail();

        String banDuration = notification.getBanDuration() != null ? notification.getBanDuration() : "Indefinite";
        String banReason = notification.getBanReason() != null ? notification.getBanReason() : "Policy violation";

        // Determine if the ban is lifetime based on duration text (common pattern)
        boolean isLifetime = banDuration.toLowerCase().contains("lifetime");
        String actionText = isLifetime
                ? "Your account access has been **permanently revoked**."
                : "Your access will be restored after the suspension period ends.";
        String headlineColor = isLifetime ? "#ef4444" : "#f59e0b"; // Red for lifetime, Amber for temporary

        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Suspension Notice</title>
                <style>
                    /* Basic reset for better compatibility */
                    body, table, td, a { -webkit-text-size-adjust: 100%%; -ms-text-size-adjust: 100%%; }
                    table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
                    /* Font import for a clean look */
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
                    /* Main styles */
                    body { margin: 0; padding: 0; background-color: #f4f7fa; font-family: 'Inter', sans-serif; }
                    .container { max-width: 600px; margin: 20px auto; padding: 20px; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e0e0e0;}
                    .header { text-align: center; padding-bottom: 20px; border-bottom: 2px solid %s; } /* Dynamic Color */
                    .logo { font-size: 28px; font-weight: 700; color: #1e293b; }
                    .content h2 { color: %s; font-size: 24px; margin-top: 0; } /* Dynamic Color */
                    .content p { color: #4b5563; line-height: 1.6; margin-bottom: 15px; }
                    .details-box { background-color: #fef2f2; padding: 15px; border-radius: 8px; margin-bottom: 20px; border: 1px dashed #fca5a5; }
                    .details-box p { margin: 5px 0; font-size: 14px; color: #b91c1c; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; font-size: 12px; color: #9ca3af; }

                    /* Responsive adjustments */
                    @media only screen and (max-width: 600px) {
                        .container { width: 100%% !important; border-radius: 0; margin: 0; }
                    }
                </style>
            </head>
            <body>
                <div style="background-color: #f4f7fa; padding: 20px 0;">
                    <div class="container">
                        <!-- Header -->
                        <div class="header" style="border-bottom: 2px solid %s;">
                            <span class="logo" style="color: %s;">CloudSync</span>
                            <p style="color: #9ca3af; font-size: 14px; margin: 5px 0 0;">Policy Enforcement Notification</p>
                        </div>

                        <!-- Content -->
                        <div class="content" style="padding: 20px 0;">
                            <h2 style="color: %s; font-size: 24px; margin-top: 0;">🛑 URGENT: Account Suspension</h2>
                            <p>Dear %s,</p>
                            <p>This is a formal notification regarding the suspension of your CloudSync account due to repeated violation of our Content and Usage Policy. Your account access has been restricted effective immediately.</p>
                            
                            <!-- Violation Details -->
                            <h3 style="color: #1e293b; font-size: 18px; margin-top: 25px; margin-bottom: 15px;">Violation Details</h3>
                            <div class="details-box">
                                <p><strong>🔴 Status:</strong> Suspended</p>
                                <p><strong>⏳ Duration:</strong> %s</p>
                                <p><strong>📄 Reason:</strong> %s</p>
                            </div>

                            <p style="font-weight: 600; color: %s;">%s</p>

                            <p>During this period, your ability to upload new files and share existing files is revoked. Please review our <a href="[Link to your Policy]" style="color: #6366f1;">Acceptable Use Policy</a> to understand the terms of service.</p>
                            
                            <p>If you believe this ban is in error or wish to appeal this decision, please reply to this email immediately.</p>

                        </div>

                        <!-- Footer -->
                        <div class="footer">
                            <p>This is an automated notification. Please reply for support regarding this matter.</p>
                            <p>&copy; %d CloudSync. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                headlineColor, // Header border color
                headlineColor, // H2 headline color
                headlineColor, // Header border color (repeated for safety/mobile)
                headlineColor, // Logo color (keeping it dynamic)
                headlineColor, // H2 headline color (repeated for safety/mobile)
                displayName,   // Dear %s,
                banDuration,   // Duration
                banReason,     // Reason
                headlineColor, // Action text color
                actionText,    // Action text
                java.time.Year.now().getValue()
        );
    }
}
