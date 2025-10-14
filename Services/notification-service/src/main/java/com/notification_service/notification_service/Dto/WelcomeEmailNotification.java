package com.notification_service.notification_service.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelcomeEmailNotification {

    @JsonProperty("email")
    private String email;

    @JsonProperty("username")
    private String username;

}