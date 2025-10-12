package Controllers;

import Configuration.RedisTopicListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final RedisMessageListenerContainer redisContainer;
    private final RedisTopicListener redisTopicListener;

    /**
     * Handles the user request to subscribe to a file's notification topic.
     * The client should send a message to /app/subscribe-file with the filename.
     */
    @MessageMapping("/subscribe-file")
    public void subscribeToFile(@Payload Map<String, String> payload) {
        String fileName = payload.get("fileName");

        // 1. Extract email from JWT/Authentication (Assumes Spring Security is configured)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.error("Unauthenticated attempt to subscribe to file notifications.");
            return;
        }

        // Assuming your Authentication principal is a JWT or has the email easily accessible
        String email = getEmailFromAuth(auth);
        if (email == null) {
            log.error("Could not extract email from user principal: {}", auth.getPrincipal());
            return;
        }

        // 2. Define the Redis Pub/Sub channel name
        // Example: my_cool_file.pdf@user@example.com
        String redisChannelName = fileName + email;

        // 3. Dynamically add the listener for this specific channel
        ChannelTopic topic = new ChannelTopic(redisChannelName);
        redisContainer.addMessageListener(redisTopicListener, topic);

        log.info("User {} successfully subscribed to Redis channel: {}", email, redisChannelName);

        // NOTE: No need to send a message back here. The subscription is handled by the framework.
    }

    // --- Helper Method for JWT Extraction ---
    private String getEmailFromAuth(Authentication auth) {
        // You'll need to adjust this based on how your JWT principal is structured.
        // This is a common pattern when using Spring Security with OAuth2/JWT.
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt) {
            return ((Jwt) principal).getClaimAsString("email"); // or "sub", depending on your token
        }
        // Fallback or handle other principal types
        return auth.getName();
    }
}