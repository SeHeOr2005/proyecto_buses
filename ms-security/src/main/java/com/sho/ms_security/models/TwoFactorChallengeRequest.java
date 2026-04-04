package com.sho.ms_security.models;

import lombok.Data;

@Data
public class TwoFactorChallengeRequest {
    private String challengeToken;
}
