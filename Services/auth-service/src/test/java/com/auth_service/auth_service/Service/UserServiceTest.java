package com.auth_service.auth_service.Service;

import com.auth_service.auth_service.DTO.WelcomeEmailNotification;
import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private UserService userService;

    private OAuth2User oAuth2User;

    @BeforeEach
    void setUp() {
        oAuth2User = mock(OAuth2User.class);
    }

    // Helper method to setup the OAuth2User mock for relevant tests
    private void setupOAuth2UserMock() {
        when(oAuth2User.getAttribute("email")).thenReturn("test@example.com");
        when(oAuth2User.getAttribute("name")).thenReturn("Test User");
        when(oAuth2User.getAttribute("sub")).thenReturn("google-id-123");
        when(oAuth2User.getAttribute("picture")).thenReturn("http://example.com/pic.jpg");
    }

    @Test
    void processOAuth2User_NewUser_ShouldCreateUser() {
        // Arrange
        setupOAuth2UserMock(); // Call helper to stub attributes

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.processOAuth2User(oAuth2User);

        // Assert
        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Test User", result.getName());
        assertEquals("google-id-123", result.getGoogleId());
        verify(queueService, times(1)).publishWelcomeEmailRequest(any(WelcomeEmailNotification.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void processOAuth2User_ExistingUser_ShouldUpdateUser() {
        // Arrange
        setupOAuth2UserMock(); // MUST setup mock here too, otherwise getAttribute("email") returns null

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("test@example.com");
        existingUser.setName("Old Name");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        User result = userService.processOAuth2User(oAuth2User);

        // Assert
        assertNotNull(result);
        assertEquals("Test User", result.getName()); // Should match the mocked OAuth Name
        assertEquals("google-id-123", result.getGoogleId());
        verify(queueService, never()).publishWelcomeEmailRequest(any());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void findByEmail_UserExists_ShouldReturnUser() {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }

    @Test
    void findById_UserExists_ShouldReturnUser() {
        // Arrange
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }
}