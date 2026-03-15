package com.example.auth.config;

import com.example.auth.service.AuthService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Al hacer login con Google (u otro OAuth2), guarda el usuario en MongoDB.
 * Se ejecuta tras la autenticación exitosa con el proveedor.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthService authService;

    public CustomOAuth2UserService(AuthService authService) {
        this.authService = authService;
    }

    @Override
public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

    OAuth2User oauth2User = super.loadUser(userRequest);
    System.out.println("OAuth attributes: " + oauth2User.getAttributes());

    String email = oauth2User.getAttribute("email");
    String name = oauth2User.getAttribute("name");
    String picture = oauth2User.getAttribute("avatar_url"); // GitHub usa avatar_url

    if (email == null || email.isBlank()) {
        email = oauth2User.getAttribute("login") + "@github.user";
    }

    if (name == null || name.isBlank()) {
        name = oauth2User.getAttribute("login");
    }

    authService.findOrCreateOAuthUser(email, name, picture);

    return oauth2User;
}
}
