package com.sho.ms_security.configurations;

import com.sho.ms_security.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Servicio OAuth2 unificado para Google y GitHub.
 * Al autenticarse con un proveedor externo, crea o actualiza el usuario en MongoDB
 * y asigna el rol CIUDADANO si es nuevo.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        String email;
        String name;
        String picture;
        String provider;

        if ("github".equalsIgnoreCase(registrationId)) {
            // HU-ENTR-1-006: GitHub — manejar email privado
            email = oauth2User.getAttribute("email");
            if (email == null || email.isBlank()) {
                String login = oauth2User.getAttribute("login");
                email = (login != null ? login : "unknown") + "@github.user";
            }
            name = oauth2User.getAttribute("name");
            if (name == null || name.isBlank()) {
                name = oauth2User.getAttribute("login");
            }
            picture = oauth2User.getAttribute("avatar_url");
            provider = "GITHUB";
        } else {
            // HU-ENTR-1-004: Google (y cualquier otro proveedor OIDC)
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            picture = oauth2User.getAttribute("picture");
            provider = registrationId.toUpperCase();
        }

        authService.findOrCreateOAuthUser(email, name, picture, provider);

        return oauth2User;
    }
}
