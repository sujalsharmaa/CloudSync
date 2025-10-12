package Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisTopicListener implements MessageListener {

    // Used to send messages to specific WebSocket destinations (i.e., specific users)
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles messages received from Redis Pub/Sub.
     * The Redis channel name is expected to be in the format: fileName+email
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String redisChannel = new String(message.getChannel(), StandardCharsets.UTF_8);
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);

            // 1. Extract the user's email from the Redis channel name
            // Assuming your file names don't contain '@'
            int lastAt = redisChannel.lastIndexOf('@');
            if (lastAt == -1) {
                log.warn("Received message on invalid channel format: {}", redisChannel);
                return;
            }
            String userEmail = redisChannel.substring(lastAt);

            // 2. Determine the WebSocket destination
            // We use /topic/notifications to keep it simple, but we target the specific user
            String websocketDestination = "/topic/notifications";

            log.info("Forwarding message from Redis channel {} to user {} on destination {}", redisChannel, userEmail, websocketDestination);

            // 3. Forward message to the specific user via their WebSocket session
            // The destination format is /user/{userEmail}/topic/notifications
            messagingTemplate.convertAndSendToUser(
                    userEmail,
                    websocketDestination,
                    payload
            );
        } catch (Exception e) {
            log.error("Error processing message from Redis", e);
        }
    }
}