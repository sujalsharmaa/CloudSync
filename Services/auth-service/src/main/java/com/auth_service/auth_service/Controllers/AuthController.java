package com.auth_service.auth_service.Controllers;

import com.auth_service.auth_service.DTO.StoragePlanResponse;
import com.auth_service.auth_service.Entity.type.Plan;
import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Service.S3Service;
import com.auth_service.auth_service.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final S3Service s3Service;

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

    @GetMapping("/getStoragePlan/{userId}")
    public ResponseEntity<Plan> getUserStoragePlan(@PathVariable String userId){
        // 1. Fetch user data (optional check is good practice)
        Optional<User> userResponse =  userService.findById(Long.valueOf(userId));

        if (userResponse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        User foundUser = userResponse.get();

        // 4. Return the ResponseEntity with the DTO and HTTP status OK
        return ResponseEntity.ok(foundUser.getPlan());
    }

    @GetMapping("/getStoragePlanAndConsumption")
    public ResponseEntity<StoragePlanResponse> getUserStoragePlanAndConsumption(@AuthenticationPrincipal User user){
        // 1. Fetch user data (optional check is good practice)
        Optional<User> userResponse =  userService.findById(user.getId());

        if (userResponse.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User foundUser = userResponse.get();

        // 2. Get storage consumed
        Long StorageConsumed = s3Service.getUserFolderSize(String.valueOf(user.getId()));

        // 3. Create the DTO instance
        StoragePlanResponse responseDTO = new StoragePlanResponse(foundUser.getPlan(), StorageConsumed);

        // 4. Return the ResponseEntity with the DTO and HTTP status OK
        return ResponseEntity.ok(responseDTO);
    }
}