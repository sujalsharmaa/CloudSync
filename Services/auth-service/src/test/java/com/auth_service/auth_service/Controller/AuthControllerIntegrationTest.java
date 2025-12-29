package com.auth_service.auth_service.Controller;// Integration Test Example
// Services/auth-service/src/test/java/com/auth_service/auth_service/Controllers/AuthControllerIntegrationTest.java

import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Repository.UserRepository;
import com.auth_service.auth_service.Security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    static {
        System.setProperty("user.timezone", "Asia/Kolkata");
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void getCurrentUser_WithValidToken_ShouldReturnUserInfo() throws Exception {
        // Arrange
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user = userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getName());

        // Act & Assert
        mockMvc.perform(get("/api/auth/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void getCurrentUser_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized());
    }
}