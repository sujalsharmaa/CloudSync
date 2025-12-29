package com.upload_download_rag_pipeline.upload_download_rag_pipeline.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisBanServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private RedisBanService redisBanService;

    @Captor
    private ArgumentCaptor<Duration> durationCaptor;

    @BeforeEach
    void setUp() {
        // BEST PRACTICE: Use lenient() for setup that is used by MOST but not ALL tests.
        // This prevents UnnecessaryStubbingException in the 'isUserBanned' tests.
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void incrementViolationAndCheckBan_FirstViolation_ShouldNotBan() {
        // Arrange
        String userId = "user123";
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act
        long count = redisBanService.incrementViolationAndCheckBan(userId, email);

        // Assert
        assertEquals(1L, count);
        verify(valueOperations, never()).set(contains("banned"), anyString(), any(Duration.class));
        verify(queueService, never()).publishBanNotification(any());
    }

    @Test
    void incrementViolationAndCheckBan_ThirdViolation_ShouldBanFor24Hours() {
        // Arrange
        String userId = "user123";
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // Act
        long count = redisBanService.incrementViolationAndCheckBan(userId, email);

        // Assert
        assertEquals(3L, count);
        verify(valueOperations, times(1)).set(
                contains("banned"),
                eq("BANNED"),
                durationCaptor.capture()
        );
        assertEquals(Duration.ofHours(24), durationCaptor.getValue());
        verify(queueService, times(1)).publishBanNotification(any());
    }

    @Test
    void incrementViolationAndCheckBan_TenthViolation_ShouldBanForOneMonth() {
        // Arrange
        String userId = "user123";
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(10L);

        // Act
        long count = redisBanService.incrementViolationAndCheckBan(userId, email);

        // Assert
        assertEquals(10L, count);
        verify(valueOperations, times(1)).set(
                contains("banned"),
                eq("BANNED"),
                durationCaptor.capture()
        );
        assertEquals(Duration.ofDays(30), durationCaptor.getValue());
    }

    @Test
    void incrementViolationAndCheckBan_TwentyFifthViolation_ShouldBanLifetime() {
        // Arrange
        String userId = "user123";
        String email = "test@example.com";
        when(valueOperations.increment(anyString())).thenReturn(25L);

        // Act
        long count = redisBanService.incrementViolationAndCheckBan(userId, email);

        // Assert
        assertEquals(25L, count);
        verify(valueOperations, times(1)).set(
                contains("banned"),
                eq("LIFETIME")
        );
        verify(queueService, times(1)).publishBanNotification(any());
    }

    @Test
    void isUserBanned_UserIsBanned_ShouldReturnTrue() {
        // Arrange
        String userId = "user123";
        // Note: This test does not use opsForValue(), so lenient() in setUp prevents the crash.
        when(redisTemplate.hasKey(contains("banned"))).thenReturn(true);

        // Act
        boolean result = redisBanService.isUserBanned(userId);

        // Assert
        assertTrue(result);
    }

    @Test
    void isUserBanned_UserNotBanned_ShouldReturnFalse() {
        // Arrange
        String userId = "user123";
        when(redisTemplate.hasKey(contains("banned"))).thenReturn(false);

        // Act
        boolean result = redisBanService.isUserBanned(userId);

        // Assert
        assertFalse(result);
    }
}