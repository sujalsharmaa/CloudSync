
package com.auth_service.auth_service.Controllers;

import com.auth_service.auth_service.DTO.StoragePlanResponse;
import com.auth_service.auth_service.Entity.type.Plan;
import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Exception.ResourceNotFoundException;
import com.auth_service.auth_service.Service.S3Service;
import com.auth_service.auth_service.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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


    @GetMapping("/getStoragePlan/{userId}")
    public ResponseEntity<Plan> getUserStoragePlan(@PathVariable Long  userId) {
        User foundUser = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        return ResponseEntity.ok(foundUser.getPlan());
    }

    @GetMapping("/getStoragePlanAndConsumption")
    public ResponseEntity<StoragePlanResponse> getUserStoragePlanAndConsumption(@AuthenticationPrincipal User user) {
        User foundUser = userService.findById(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", user.getId()));

        Long storageConsumed = s3Service.getUserFolderSize(String.valueOf(user.getId()));
        StoragePlanResponse responseDTO = new StoragePlanResponse(foundUser.getPlan(), storageConsumed);

        return ResponseEntity.ok(responseDTO);
    }
}
