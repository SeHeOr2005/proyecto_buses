package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Maneja el flujo OAuth de GitHub: recibe el code, lo cambia por token y obtiene el usuario.
 * Redirige al frontend con token, name, email y picture.
 */
@RestController
public class GithubAuthController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AuthService authService;

    public GithubAuthController(AuthService authService) {
        this.authService = authService;
    }

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    @Value("${frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @GetMapping("/auth/github/callback")
    public ResponseEntity<Void> callback(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?error=github_no_code"))
                    .build();
        }

        String accessToken = exchangeCodeForToken(code);
        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?error=github_token_failed"))
                    .build();
        }

        Map<String, Object> user = getUserFromGitHub(accessToken);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendUrl + "/login?error=github_user_failed"))
                    .build();
        }

        String name = (String) user.get("name");
        if (name == null || name.isBlank()) {
            name = (String) user.get("login");
        }
        String email = (String) user.get("email");
        if (email == null || email.isBlank()) {
            email = (String) user.get("login") + "@github.user";
        }
        String picture = (String) user.get("avatar_url");

        User dbUser = authService.findOrCreateOAuthUser(email, name, picture);

        String appToken = "github-jwt-" + accessToken.substring(0, Math.min(20, accessToken.length()));

        String redirectTo = UriComponentsBuilder.fromHttpUrl(frontendUrl + "/auth/callback")
                .queryParam("token", appToken)
                .queryParam("name", dbUser.getName() != null ? dbUser.getName() : "")
                .queryParam("email", dbUser.getEmail())
                .queryParam("picture", dbUser.getPicture() != null ? dbUser.getPicture() : "")
                .build()
                .toUriString();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectTo))
                .build();
    }

    private String exchangeCodeForToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://github.com/login/oauth/access_token",
                request,
                Map.class
        );

        if (response.getBody() != null && response.getBody().containsKey("access_token")) {
            return (String) response.getBody().get("access_token");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserFromGitHub(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                request,
                Map.class
        );
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private String fetchPrimaryEmail(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/vnd.github+json");

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map[]> response = restTemplate.exchange(
                "https://api.github.com/user/emails",
                HttpMethod.GET,
                request,
                (Class<Map[]>) (Class<?>) Map[].class
        );
        if (response.getBody() == null) return null;
        for (Map<String, Object> entry : response.getBody()) {
            Boolean primary = (Boolean) entry.get("primary");
            Boolean verified = (Boolean) entry.get("verified");
            String email = (String) entry.get("email");
            if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified) && email != null) {
                return email;
            }
        }
        // Fallback: return any verified email
        for (Map<String, Object> entry : response.getBody()) {
            Boolean verified = (Boolean) entry.get("verified");
            String email = (String) entry.get("email");
            if (Boolean.TRUE.equals(verified) && email != null) {
                return email;
            }
        }
        return null;
    }
}
