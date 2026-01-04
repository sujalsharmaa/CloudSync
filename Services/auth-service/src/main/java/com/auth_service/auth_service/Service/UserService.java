package com.auth_service.auth_service.Service;

import com.auth_service.auth_service.DTO.WelcomeEmailNotification;
import com.auth_service.auth_service.Entity.type.User;
import com.auth_service.auth_service.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;
    private final QueueService queueService;

    @CacheEvict(value = "user_email", key = "#oAuth2User.getAttribute('email')")
    public User processOAuth2User(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");

        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            user.setName(name);
            user.setPicture(picture);
            user.setGoogleId(googleId);
            return userRepository.save(user);
        } else {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setPicture(picture);
            newUser.setGoogleId(googleId);
            WelcomeEmailNotification welcomeEmailNotification = new WelcomeEmailNotification(email, name);
            queueService.publishWelcomeEmailRequest(welcomeEmailNotification);
            return userRepository.save(newUser);
        }
    }

    @Cacheable(value = "user_email", key = "#email")
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}