package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.BanNotification; // Import the new DTO
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBanService {

    private static final String VIOLATION_KEY_PREFIX = "user:violation_count:";
    private static final String BAN_KEY_PREFIX = "user:banned:";
    private static final String LIFETIME_BAN_VALUE = "LIFETIME";

    private final StringRedisTemplate redisTemplate;
    private final QueueService queueService; // <-- INJECT QUEUE SERVICE
    // Placeholder for a service to fetch user details (email/username)
    // private final UserService userService;

    /**
     * Increments the violation count for a user and applies a ban if thresholds are met.
     * @param userId The ID of the user who violated the policy.
     * @param email The email of the user (passed from JWT)
     * @return The updated violation count.
     */
    public long incrementViolationAndCheckBan(String userId, String email) {
        String violationKey = VIOLATION_KEY_PREFIX + userId;

        // 1. Increment the violation count
        Long newCount = redisTemplate.opsForValue().increment(violationKey);

        if (newCount == null) {
            log.error("Failed to increment violation count for user: {}", userId);
            return 0;
        }

        // 2. Check thresholds and apply bans
        Duration banDuration = null;
        String banValue = "BANNED";
        String durationString = null;
        String reason = null;

        if (newCount == 5) {
            durationString = "24 hours";
            banDuration = Duration.ofHours(24);
            reason = "File policy violation count reached 5.";
        } else if (newCount == 10) {
            durationString = "1 month";
            banDuration = Duration.ofDays(30);
            reason = "File policy violation count reached 10.";
        } else if (newCount == 20) {
            durationString = "3 months";
            banDuration = Duration.ofDays(90);
            reason = "File policy violation count reached 20.";
        } else if (newCount >= 25) {
            durationString = LIFETIME_BAN_VALUE;
            banDuration = null; // No TTL for lifetime ban
            banValue = LIFETIME_BAN_VALUE;
            reason = "File policy violation count reached 25. Account permanently banned.";
        }

        // Apply ban and publish notification
        if (reason != null) {
            String username = "User"; // TODO: Implement lookup for username if needed

            if (banValue.equals(LIFETIME_BAN_VALUE)) {
                redisTemplate.opsForValue().set(BAN_KEY_PREFIX + userId, banValue);
            } else {
                redisTemplate.opsForValue().set(BAN_KEY_PREFIX + userId, banValue, banDuration);
            }

            log.warn("User {} banned for {}. Reason: {}", userId, durationString, reason);

            // --- PUBLISH NOTIFICATION ---
            BanNotification notification = BanNotification.builder()
                    .userId(userId)
                    .email(email)
                    .username(username)
                    .banDuration(durationString)
                    .banReason(reason)
                    .build();

            queueService.publishBanNotification(notification);
            // ----------------------------
        }

        return newCount;
    }

    /**
     * Checks if a user is currently banned.
     * @param userId The user ID.
     * @return true if the user is banned, false otherwise.
     */
    public boolean isUserBanned(String userId) {
        return redisTemplate.hasKey(BAN_KEY_PREFIX + userId);
    }
}