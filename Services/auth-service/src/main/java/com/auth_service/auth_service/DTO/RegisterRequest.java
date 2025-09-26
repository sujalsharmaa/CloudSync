package com.auth_service.auth_service.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String name;
    private String email;
}

