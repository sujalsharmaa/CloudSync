package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import com.upload_download_rag_pipeline.upload_download_rag_pipeline.Dto.BanNotification;
import com.upload_download_rag_pipeline.upload_download_rag_pipeline.config.BanProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisBanService {

    private final StringRedisTemplate redisTemplate;
    private final QueueService queueService;
    private final BanProperties banProperties;

    public long incrementViolationAndCheckBan(String userId, String email) {

        String violationKey =
                banProperties.getRedis().getViolationKeyPrefix() + userId;

        Long newCount =
                redisTemplate.opsForValue().increment(violationKey);

        if (newCount == null) {
            log.error("Failed to increment violation count for user {}", userId);
            return 0;
        }

        banProperties.getRules().stream()
                .filter(rule -> newCount == rule.getCount())
                .findFirst()
                .ifPresent(rule -> applyBan(rule, userId, email));

        return newCount;
    }

    private void applyBan(BanProperties.Rule rule, String userId, String email) {

        String banKey =
                banProperties.getRedis().getBanKeyPrefix() + userId;

        String banValue =
                rule.isLifetime()
                        ? banProperties.getRedis().getLifetimeValue()
                        : "BANNED";

        if (rule.isLifetime()) {
            redisTemplate.opsForValue().set(banKey, banValue);
        } else {
            redisTemplate.opsForValue().set(banKey, banValue, rule.getTtl());
        }

        log.warn("User {} banned | duration={} | reason={}",
                userId, rule.getDuration(), rule.getReason());

        BanNotification notification = BanNotification.builder()
                .userId(userId)
                .email(email)
                .username("User")
                .banDuration(rule.getDuration())
                .banReason(rule.getReason())
                .build();

        queueService.publishBanNotification(notification);
    }

    public boolean isUserBanned(String userId) {
        return redisTemplate.hasKey(
                banProperties.getRedis().getBanKeyPrefix() + userId
        );
    }
}
