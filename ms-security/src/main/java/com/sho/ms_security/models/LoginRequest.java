package com.sho.ms_security.models;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String recaptchaToken;
}
