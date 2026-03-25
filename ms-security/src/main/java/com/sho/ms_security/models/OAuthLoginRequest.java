package com.sho.ms_security.models;

import lombok.Data;

@Data
public class OAuthLoginRequest {
    private String firebaseIdToken;
}

