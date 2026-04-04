package com.sho.ms_security.services;

import com.sho.ms_security.models.User;
import com.sho.ms_security.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class PasswordRecoveryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${password-recovery.token-expiration-minutes:30}")
    private Long tokenExpirationMinutes;

    public void requestPasswordRecovery(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        User user = userRepository.getUserByEmail(email.trim());
        if (user == null || Boolean.FALSE.equals(user.getActive())) {
            return;
        }

        String token = UUID.randomUUID().toString();
        Date expiration = Date.from(Instant.now().plus(tokenExpirationMinutes, ChronoUnit.MINUTES));

        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiration(expiration);
        userRepository.save(user);

        String recoveryLink = String.format("%s/auth/forgot-password?token=%s", frontendBaseUrl, token);
        emailNotificationService.sendPasswordRecoveryNotification(
                user.getEmail(),
                user.getName(),
                recoveryLink,
                tokenExpirationMinutes
        );
    }

    public boolean resetPassword(String token, String newPassword) {
        if (!StringUtils.hasText(token) || !isPasswordValid(newPassword)) {
            return false;
        }

        User user = userRepository.getUserByResetPasswordToken(token.trim());
        if (user == null || user.getResetPasswordTokenExpiration() == null) {
            return false;
        }

        Date now = new Date();
        if (user.getResetPasswordTokenExpiration().before(now)) {
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiration(null);
            userRepository.save(user);
            return false;
        }

        user.setPassword(encryptionService.convertSHA256(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiration(null);
        userRepository.save(user);
        return true;
    }

    private boolean isPasswordValid(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[^A-Za-z0-9].*");
    }
}
