package com.auth_service.auth_service.Controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ProtectedController {

    // This endpoint is configured as a protected route.
    // It requires a valid JWT token in the Authorization header to be accessed.
    @GetMapping("/protected/resource")
    public ResponseEntity<String> getProtectedResource() {
        // You can get the authenticated user's details from the SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        return ResponseEntity.ok("Hello, " + userEmail + "! You have accessed a protected resource.");
    }

    // This is a public route to demonstrate the SecurityConfig's permitAll() method.
    @GetMapping("/hello")
    public ResponseEntity<String> getHello() {
        return ResponseEntity.ok("Hello! This is a public, unsecured endpoint.");
    }
}
