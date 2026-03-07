package com.example.auth.service;

import com.example.auth.model.LoginRequest;
import com.example.auth.model.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String VALID_EMAIL = "admin@test.com";
    private static final String VALID_PASSWORD = "123456";
    private static final String FAKE_TOKEN = "fake-jwt-token";

    public LoginResponse login(LoginRequest request) {
        if (VALID_EMAIL.equals(request.getEmail()) && VALID_PASSWORD.equals(request.getPassword())) {
            return new LoginResponse("Login exitoso", FAKE_TOKEN);
        }
        return null;
    }
}
