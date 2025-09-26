package com.auth_service.auth_service.Controllers;

import com.auth_service.auth_service.Entity.type.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/user")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("name", user.getName());
        userInfo.put("picture", user.getPicture());

        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Since we're using stateless JWT, logout is handled on the client side
        return ResponseEntity.ok().body("Logged out successfully");
    }

    @GetMapping("/login/google")
    public void redirectToGoogle() {
        // This will be handled by Spring Security OAuth2
    }
}