package com.sho.ms_security.controllers;

import com.sho.ms_security.models.User;
import com.sho.ms_security.models.dto.LoginRequest;
import com.sho.ms_security.models.dto.LoginResponse;
import com.sho.ms_security.models.dto.RegisterRequest;
import com.sho.ms_security.services.AuthService;
import com.sho.ms_security.services.ValidatorsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints de autenticación: registro, login, perfil y cambio de contraseña.
 * Rutas públicas: POST /api/auth/register, POST /api/auth/login
 * Rutas protegidas (JWT): GET /api/auth/me, POST /api/auth/change-password
 */
@RestController
@CrossOrigin
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ValidatorsService validatorsService;

    /** HU-ENTR-1-008: Login con email y contraseña */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenciales inválidas"));
        }
        return ResponseEntity.ok(response);
    }

    /** HU-ENTR-1-007: Registro con email y contraseña */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String result = authService.register(request);
        if (result.startsWith("CONFLICT:")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", result.substring(9)));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", result));
    }

    /** Perfil del usuario autenticado (requiere JWT válido) */
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        User user = validatorsService.getUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autorizado: token inválido o sin permisos"));
        }
        return ResponseEntity.ok(Map.of(
                "id",       user.getId(),
                "name",     user.getName() != null ? user.getName() : "",
                "lastName", user.getLastName() != null ? user.getLastName() : "",
                "email",    user.getEmail(),
                "picture",  user.getPicture() != null ? user.getPicture() : "",
                "provider", user.getProvider() != null ? user.getProvider() : "LOCAL"
        ));
    }

    /** Cambio de contraseña (requiere JWT válido, solo usuarios LOCAL) */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        User user = validatorsService.getUser(request);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No autorizado: token inválido o sin permisos"));
        }
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String result = authService.changePassword(user, oldPassword, newPassword);
        if (result.startsWith("ERROR:")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", result.substring(6)));
        }
        return ResponseEntity.ok(Map.of("message", result));
    }
}
