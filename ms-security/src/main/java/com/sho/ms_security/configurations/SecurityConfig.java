package com.sho.ms_security.configurations;

import com.sho.ms_security.models.User;
import com.sho.ms_security.services.AuthService;
import com.sho.ms_security.services.JwtService;
import com.sho.ms_security.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler((request, response, authentication) -> {
                    String email = null;
                    String name = null;
                    String picture = null;
                    String provider = "GOOGLE";

                    if (authentication.getPrincipal() instanceof OAuth2User oauthUser) {
                        email   = oauthUser.getAttribute("email");
                        name    = oauthUser.getAttribute("name");
                        picture = oauthUser.getAttribute("picture");

                        // GitHub usa avatar_url en lugar de picture
                        if (oauthUser.getAttribute("avatar_url") != null) {
                            provider = "GITHUB";
                            picture  = oauthUser.getAttribute("avatar_url");
                            if (email == null || email.isBlank()) {
                                String login = oauthUser.getAttribute("login");
                                if (login != null) email = login + "@github.user";
                                if (name == null || name.isBlank()) name = login;
                            }
                        }
                    }

                    if (email == null || email.isBlank()) {
                        response.sendRedirect(frontendUrl + "/login?error=oauth2_no_email");
                        return;
                    }

                    final String emailNorm = email.trim().toLowerCase();
                    final String finalName = name;
                    final String finalPicture = picture;
                    final String finalProvider = provider;

                    // Buscar usuario; si no existe crearlo (fallback por si CustomOAuth2UserService falló)
                    User user = userRepository.findByEmail(emailNorm)
                            .orElseGet(() -> {
                                System.out.println("[OAuth2] Usuario no encontrado tras autenticación, creando: " + emailNorm);
                                return authService.findOrCreateOAuthUser(emailNorm, finalName, finalPicture, finalProvider);
                            });

                    String token = jwtService.generateToken(user);
                    String encodedName  = URLEncoder.encode(user.getName()  != null ? user.getName()  : "", StandardCharsets.UTF_8);
                    String encodedEmail = URLEncoder.encode(emailNorm, StandardCharsets.UTF_8);
                    response.sendRedirect(frontendUrl + "/auth-callback?token=" + token
                            + "&name=" + encodedName + "&email=" + encodedEmail);
                })
                .failureHandler((request, response, exception) -> {
                    System.err.println("[OAuth2] Fallo: " + exception.getMessage());
                    response.sendRedirect(frontendUrl + "/login?error=oauth2");
                })
            )
            .logout(logout -> logout
                .logoutSuccessUrl(frontendUrl + "/login")
                .permitAll()
            );

        return http.build();
    }
}
