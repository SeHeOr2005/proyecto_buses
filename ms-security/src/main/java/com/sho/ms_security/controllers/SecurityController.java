package com.sho.ms_security.controllers;

import com.sho.ms_security.models.OAuthLoginRequest;
import com.sho.ms_security.models.User;
import com.sho.ms_security.services.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/security")
public class SecurityController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityController.class);

    @Autowired
    private SecurityService theSecurityService;

    @PostMapping("login")
    public HashMap<String, Object> login(@RequestBody User theNewUser,
                                         final HttpServletResponse response) throws IOException {
        HashMap<String, Object> theResponse = new HashMap<>();
        String token = this.theSecurityService.login(theNewUser);
        if (token != null) {
            theResponse.put("token", token);
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        return theResponse;
    }

    @PostMapping("oauth/login")
    public ResponseEntity<?> oauthLogin(@RequestBody OAuthLoginRequest request) {
        if (request == null || request.getFirebaseIdToken() == null || request.getFirebaseIdToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "firebaseIdToken es obligatorio"));
        }

        try {
            HashMap<String, Object> response = this.theSecurityService.oauthLogin(request.getFirebaseIdToken());
            if (response == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token OAuth invalido"));
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOGGER.error("Error en /security/oauth/login", e);
            if (e instanceof IllegalArgumentException) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Proveedor OAuth no permitido", "detail", e.getMessage()));
            }
            if (e.getClass().getSimpleName().contains("FirebaseAuthException")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token OAuth invalido", "detail", e.getMessage()));
            }
            if (e instanceof IOException || e instanceof IllegalStateException) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Configuracion de Firebase incompleta en backend",
                                "detail", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No fue posible autenticar con OAuth", "detail", e.getMessage()));
        }
    }

    @GetMapping("me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token requerido"));
        }

        Map<String, Object> payload = this.theSecurityService.getUserPayloadFromToken(token);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token invalido"));
        }
        return ResponseEntity.ok(payload);
    }

    @PostMapping("logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = extractBearerToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token requerido"));
        }

        boolean revoked = this.theSecurityService.logout(token);
        if (!revoked) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No existe una sesion activa para el token"));
        }
        return ResponseEntity.ok(Map.of("message", "Sesion cerrada"));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
