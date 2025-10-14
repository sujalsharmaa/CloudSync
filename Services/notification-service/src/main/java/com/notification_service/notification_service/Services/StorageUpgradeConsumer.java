package com.notification_service.notification_service.Services;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification_service.notification_service.Dto.StorageUpgradeNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageUpgradeConsumer {

    private final ObjectMapper objectMapper;
    private final MailService mailService;

    @KafkaListener(
            topics = "storage-upgrade-topic",
            groupId = "rag-pipeline-group-notification"
    )
    public void consumeStorageUpgrade(String message) {

        try {
            StorageUpgradeNotification notification = objectMapper.readValue(message, StorageUpgradeNotification.class);
            log.info("Parsed storage upgrade notification for user: {}", notification.getEmail());

            // Build upgrade confirmation email
            String subject = "🚀 Success! Your CloudSync Plan is Now the " + notification.getNewPlan() + " Plan!";
            String body = buildUpgradeEmailBody(notification);

            // Send email
            mailService.sendEmail(notification.getEmail(), subject, body);
            log.info("Successfully sent storage upgrade email to {}", notification.getEmail());

        } catch (Exception e) {
            log.error("Error processing storage upgrade notification: {}", message, e);
        }
    }

    private String buildUpgradeEmailBody(StorageUpgradeNotification notification) {
        // Safe access to notification properties, providing necessary defaults
        String displayName = notification.getUsername() != null && !notification.getUsername().isBlank()
                ? notification.getUsername()
                : notification.getEmail();

        String newPlan = notification.getNewPlan() != null ? notification.getNewPlan() : "N/A";
        Integer newStorageGB = notification.getNewStorageGB() != null ? notification.getNewStorageGB() : 0;

        // Only use DTO fields - removed oldStorageGB, price, and billingCycle references.

        String formattedUpgradeDate = notification.getUpgradeDate() != null ? notification.getUpgradeDate() :
                new SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(new Date());

        // --- HTML EMAIL BODY START ---
        return String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Storage Upgrade Success</title>
                <style>
                    /* Basic reset for better compatibility */
                    body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }
                    table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }
                    img { -ms-interpolation-mode: bicubic; }
                    /* Font import for a clean look */
                    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap');
                    /* Main styles */
                    body { margin: 0; padding: 0; background-color: #f4f7fa; font-family: 'Inter', sans-serif; }
                    .container { max-width: 600px; margin: 20px auto; padding: 20px; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); border: 1px solid #e0e0e0;}
                    .header { text-align: center; padding-bottom: 20px; border-bottom: 2px solid #10b981; } /* Green accent */
                    .logo { font-size: 28px; font-weight: 700; color: #1e293b; }
                    .content h2 { color: #1e293b; font-size: 24px; margin-top: 0; }
                    .content p { color: #4b5563; line-height: 1.6; margin-bottom: 15px; }
                    .cta-button { 
                        display: inline-block; 
                        padding: 12px 25px; 
                        margin: 20px 0;
                        background-color: #10b981; /* Green CTA button */
                        color: #ffffff; 
                        font-weight: 600; 
                        text-decoration: none; 
                        border-radius: 8px;
                        transition: background-color 0.3s;
                    }
                    .summary-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
                    .summary-table th, .summary-table td { padding: 10px; text-align: left; border-bottom: 1px solid #e5e7eb; font-size: 14px; }
                    .summary-table th { background-color: #f3f4f6; color: #374151; font-weight: 600; }
                    .highlight { font-weight: 700; color: #10b981; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #e5e7eb; font-size: 12px; color: #9ca3af; }
                    
                    /* Responsive adjustments */
                    @media only screen and (max-width: 600px) {
                        .container { width: 100% !important; border-radius: 0; margin: 0; }
                        .cta-button { display: block; text-align: center; }
                        .summary-table, .summary-table th, .summary-table td { display: block; width: 100%; box-sizing: border-box; }
                        .summary-table th { text-align: center; }
                        .summary-table td { text-align: right; border-bottom: none; }
                        .summary-table tr { border-bottom: 1px solid #e5e7eb; display: block; }
                    }
                </style>
            </head>
            <body>
                <div style="background-color: #f4f7fa; padding: 20px 0;">
                    <div class="container">
                        <!-- Header -->
                        <div class="header">
                            <span class="logo" style="color: #6366f1;">CloudSync</span>
                            <p style="color: #9ca3af; font-size: 14px; margin: 5px 0 0;">Secure Cloud Storage, Simplified.</p>
                        </div>

                        <!-- Content -->
                        <div class="content" style="padding: 20px 0;">
                            <h2 style="color: #1e293b; font-size: 24px; margin-top: 0;">🎉 Success, %s!</h2>
                            <p>We're happy to confirm that your CloudSync **storage plan has been successfully upgraded!** Your new storage capacity is available immediately.</p>
                            
                            <h3 style="color: #1e293b; font-size: 18px; margin-top: 25px; margin-bottom: 15px;">📦 Upgrade Summary</h3>
                            
                            <table class="summary-table">
                                <tr>
                                    <th style="width: 40%%;">Detail</th>
                                    <th style="width: 60%%;">Value</th>
                                </tr>
                                <tr>
                                    <td>New Plan</td>
                                    <td><span class="highlight">%s</span></td>
                                </tr>
                                <tr>
                                    <td>Total Storage</td>
                                    <td><span class="highlight">%d GB</span></td>
                                </tr>
                                <tr>
                                    <td>Upgrade Date</td>
                                    <td>%s</td>
                                </tr>
                            </table>

                            <h3 style="color: #1e293b; font-size: 18px; margin-top: 25px; margin-bottom: 15px;">🚀 Enjoy Your New Capacity</h3>
                            <p>You can start leveraging your extra space right away. Here are some benefits of your new plan:</p>
                            <ul style="color: #4b5563; padding-left: 20px;">
                                <li>**%s GB** of dedicated, secure storage.</li>
                                <li>Higher single file upload limits (e.g., up to %s).</li>
                                <li>Priority access to customer support.</li>
                                <li>Access to all premium features!</li>
                            </ul>

                            <!-- CTA Button -->
                            <a href="[Your App Login URL]" class="cta-button">
                                Start Uploading Now
                            </a>
                            
                            <!-- Removed Billing Note -->
                            <p style="font-size: 14px; color: #374151; margin-top: 30px;">
                                Thank you for upgrading! You can view and manage your subscription details anytime in your account settings.
                            </p>

                        </div>

                        <!-- Footer -->
                        <div class="footer">
                            <p>Thank you for trusting CloudSync with your valuable files!</p>
                            <p>Need help? Visit our <a href="[Your Help Center URL]" style="color: #6366f1; text-decoration: none;">Help Center</a> or reply to this email.</p>
                            <p>&copy; %d CloudSync. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
                displayName,       // H2 Welcome, %s!
                newPlan,           // New Plan (in table)
                newStorageGB,      // Total Storage (in table)
                formattedUpgradeDate, // Upgrade Date (in table)
                newStorageGB,      // Total Storage (in list)
                getMaxFileSize(newPlan), // Max File Size (in list)
                java.time.Year.now().getValue()
        );
        // --- HTML EMAIL BODY END ---
    }

    // Helper function to determine max file size based on plan
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
