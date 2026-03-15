package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final TokenAuthenticationFilter tokenAuthenticationFilter;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                         TokenAuthenticationFilter tokenAuthenticationFilter) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        System.out.println("[SecurityConfig] loaded");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Login/registro abiertos
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        // Endpoints que requieren token válido
                        .requestMatchers("/api/auth/me", "/api/auth/change-password").authenticated()
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Rutas públicas (HTML/CSS/JS) y páginas de login
                        .requestMatchers( "/","/login","/oauth2/**","/login/oauth2/**").permitAll()
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().permitAll()

                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .defaultSuccessUrl("http://localhost:4200/home", true)
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("http://localhost:4200/login")
                        .permitAll()
                );

        return http.build();
    }
}
