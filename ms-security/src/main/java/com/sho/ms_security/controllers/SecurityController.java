package com.sho.ms_security.controllers;

import com.sho.ms_security.models.LoginRequest;
import com.sho.ms_security.models.OAuthLoginRequest;
import com.sho.ms_security.models.PasswordRecoveryRequest;
import com.sho.ms_security.models.ResetPasswordRequest;
import com.sho.ms_security.models.TwoFactorChallengeRequest;
import com.sho.ms_security.models.TwoFactorVerifyRequest;
import com.sho.ms_security.models.User;
import com.sho.ms_security.services.PasswordRecoveryService;
import com.sho.ms_security.services.RecaptchaVerificationService;
import com.sho.ms_security.services.SecurityService;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private RecaptchaVerificationService recaptchaVerificationService;

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "email y password son obligatorios"));
        }

        boolean recaptchaValid = this.recaptchaVerificationService.verifyLoginToken(
                loginRequest.getRecaptchaToken(),
                request.getRemoteAddr()
        );
        if (!recaptchaValid) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Fallo la validacion de reCAPTCHA"));
        }

        User credentials = new User(loginRequest.getEmail(), loginRequest.getPassword());
        Map<String, Object> loginResult = this.theSecurityService.login(credentials);
        if (loginResult == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (Boolean.TRUE.equals(loginResult.get("requires2fa"))) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(loginResult);
        }
        return ResponseEntity.ok(loginResult);
    }

    @PostMapping("2fa/verify")
    public ResponseEntity<?> verifyTwoFactor(@RequestBody TwoFactorVerifyRequest request) {
        if (request == null || request.getChallengeToken() == null || request.getChallengeToken().isBlank()
                || request.getCode() == null || request.getCode().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "challengeToken y code son obligatorios"));
        }

        Map<String, Object> result = this.theSecurityService.verifyTwoFactorCode(
                request.getChallengeToken(), request.getCode()
        );

        String status = (String) result.get("status");
        if ("OK".equals(status)) {
            return ResponseEntity.ok(Map.of("token", result.get("token")));
        }
        if ("INVALID_CODE".equals(status)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Codigo incorrecto", "attemptsLeft", result.get("attemptsLeft")));
        }
        if ("LOCKED".equals(status)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Intentos agotados", "attemptsLeft", 0));
        }
        if ("EXPIRED".equals(status)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Codigo expirado"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Desafio 2FA invalido"));
    }

    @PostMapping("2fa/resend")
    public ResponseEntity<?> resendTwoFactor(@RequestBody TwoFactorChallengeRequest request) {
        if (request == null || request.getChallengeToken() == null || request.getChallengeToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "challengeToken es obligatorio"));
        }

        Map<String, Object> result = this.theSecurityService.resendTwoFactorCode(request.getChallengeToken());
        String status = (String) result.get("status");

        if ("RESENT".equals(status)) {
            return ResponseEntity.ok(Map.of(
                    "message", "Codigo reenviado",
                    "expiresAt", result.get("expiresAt"),
                    "remainingAttempts", result.get("remainingAttempts"),
                    "maskedEmail", result.get("maskedEmail")
            ));
        }
        if ("WAIT".equals(status)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Debes esperar para reenviar", "secondsLeft", result.get("secondsLeft")));
        }
        if ("LOCKED".equals(status)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Intentos agotados"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Desafio 2FA invalido"));
    }

    @PostMapping("2fa/cancel")
    public ResponseEntity<?> cancelTwoFactor(@RequestBody TwoFactorChallengeRequest request) {
        if (request == null || request.getChallengeToken() == null || request.getChallengeToken().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "challengeToken es obligatorio"));
        }
        this.theSecurityService.cancelPendingTwoFactor(request.getChallengeToken());
        return ResponseEntity.ok(Map.of("message", "Desafio 2FA cancelado"));
    }

    @PostMapping("2fa/cancel/{challengeToken}")
    public ResponseEntity<?> cancelTwoFactorFromUnload(@PathVariable String challengeToken) {
        this.theSecurityService.cancelPendingTwoFactor(challengeToken);
        return ResponseEntity.ok(Map.of("message", "Desafio 2FA cancelado"));
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

    @PostMapping("password-recovery/request")
    public ResponseEntity<?> requestPasswordRecovery(@RequestBody PasswordRecoveryRequest request,
                                                     HttpServletRequest httpRequest) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email es obligatorio"));
        }

        boolean recaptchaValid = this.recaptchaVerificationService.verifyToken(
                request.getRecaptchaToken(),
                "password_recovery_request",
                httpRequest.getRemoteAddr()
        );
        if (!recaptchaValid) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Fallo la validacion de reCAPTCHA"));
        }

        this.passwordRecoveryService.requestPasswordRecovery(request.getEmail());
        return ResponseEntity.ok(Map.of(
                "message", "Si el correo está registrado, enviaremos un enlace para recuperar tu contraseña."
        ));
    }

    @PostMapping("password-recovery/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request,
                                           HttpServletRequest httpRequest) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()
                || request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "token y newPassword son obligatorios"));
        }

        boolean recaptchaValid = this.recaptchaVerificationService.verifyToken(
                request.getRecaptchaToken(),
                "password_recovery_reset",
                httpRequest.getRemoteAddr()
        );
        if (!recaptchaValid) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Fallo la validacion de reCAPTCHA"));
        }

        boolean resetOk = this.passwordRecoveryService.resetPassword(request.getToken(), request.getNewPassword());
        if (!resetOk) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "No fue posible restablecer la contraseña"));
        }

        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
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
