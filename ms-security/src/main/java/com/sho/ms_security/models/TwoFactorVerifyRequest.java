package com.sho.ms_security.models;

import lombok.Data;

@Data
public class TwoFactorVerifyRequest {
    private String challengeToken;
    private String code;
}
