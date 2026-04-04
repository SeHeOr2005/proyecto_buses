package com.sho.ms_security.models;

import lombok.Data;

@Data
public class PasswordRecoveryRequest {
    private String email;
    private String recaptchaToken;
}
