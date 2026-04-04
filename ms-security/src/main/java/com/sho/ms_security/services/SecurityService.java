package com.sho.ms_security.services;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.Session;
import com.sho.ms_security.models.User;
import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.repositories.RoleRepository;
import com.sho.ms_security.repositories.SessionRepository;
import com.sho.ms_security.repositories.UserRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SecurityService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    @Autowired
    private JwtService theJwtService;

    @Autowired
    private FirebaseAuthService firebaseAuthService;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${oauth.allowed-providers:google.com,microsoft.com,github.com}")
    private String allowedOAuthProviders;

    @Value("${twofactor.enabled:true}")
    private boolean twoFactorEnabled;

    @Value("${twofactor.code-expiration-seconds:180}")
    private Long twoFactorCodeExpirationSeconds;

    @Value("${twofactor.max-attempts:3}")
    private Integer twoFactorMaxAttempts;

    public Map<String, Object> login(User theNewUser) {
        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());
        if (theActualUser != null &&
                !Boolean.FALSE.equals(theActualUser.getActive()) &&
                StringUtils.hasText(theActualUser.getPassword()) &&
                theActualUser.getPassword().equals(
                        theEncryptionService.convertSHA256(theNewUser.getPassword()))) {

            if (twoFactorEnabled) {
                Session pendingTwoFactorSession = createPendingTwoFactorSession(theActualUser, "password");
                Map<String, Object> challenge = new HashMap<>();
                challenge.put("requires2fa", true);
                challenge.put("challengeToken", pendingTwoFactorSession.getToken());
                challenge.put("maskedEmail", maskEmail(theActualUser.getEmail()));
                challenge.put("expiresAt", pendingTwoFactorSession.getExpiration().getTime());
                challenge.put("remainingAttempts", pendingTwoFactorSession.getTwoFactorAttemptsLeft());
                return challenge;
            }

            String token = theJwtService.generateToken(theActualUser);
            createSession(theActualUser, token, "password");
            return Map.of("token", token);
        }
        return null;
    }

    public Map<String, Object> verifyTwoFactorCode(String challengeToken, String code) {
        Session session = this.theSessionRepository.findPendingTwoFactorByToken(challengeToken);
        if (session == null || session.getUser() == null || session.getRevokedAt() != null) {
            return Map.of("status", "INVALID");
        }

        Date now = new Date();
        if (session.getExpiration() == null || session.getExpiration().before(now)) {
            session.setRevokedAt(now);
            this.theSessionRepository.save(session);
            return Map.of("status", "EXPIRED");
        }

        if (!StringUtils.hasText(code) || !code.equals(session.getCode2FA())) {
            int attemptsLeft = session.getTwoFactorAttemptsLeft() != null
                    ? Math.max(0, session.getTwoFactorAttemptsLeft() - 1)
                    : 0;
            session.setTwoFactorAttemptsLeft(attemptsLeft);

            if (attemptsLeft == 0) {
                session.setRevokedAt(now);
                this.theSessionRepository.save(session);
                return Map.of("status", "LOCKED", "attemptsLeft", 0);
            }

            this.theSessionRepository.save(session);
            return Map.of("status", "INVALID_CODE", "attemptsLeft", attemptsLeft);
        }

        String token = this.theJwtService.generateToken(session.getUser());
        session.setToken(token);
        session.setJti(this.theJwtService.getTokenId(token));
        session.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration));
        session.setCode2FA(null);
        session.setTwoFactorAttemptsLeft(null);
        session.setTwoFactorVerifiedAt(now);
        this.theSessionRepository.save(session);

        return Map.of("status", "OK", "token", token);
    }

    public Map<String, Object> resendTwoFactorCode(String challengeToken) {
        Session session = this.theSessionRepository.findPendingTwoFactorByToken(challengeToken);
        if (session == null || session.getUser() == null || session.getRevokedAt() != null) {
            return Map.of("status", "INVALID");
        }

        Date now = new Date();
        if (session.getExpiration() != null && session.getExpiration().after(now)) {
            long secondsLeft = Math.max(0, (session.getExpiration().getTime() - now.getTime()) / 1000);
            return Map.of("status", "WAIT", "secondsLeft", secondsLeft);
        }

        if (session.getTwoFactorAttemptsLeft() == null || session.getTwoFactorAttemptsLeft() <= 0) {
            session.setRevokedAt(now);
            this.theSessionRepository.save(session);
            return Map.of("status", "LOCKED");
        }

        String newCode = generateSixDigitCode();
        Date newExpiration = new Date(System.currentTimeMillis() + (twoFactorCodeExpirationSeconds * 1000));
        session.setCode2FA(newCode);
        session.setExpiration(newExpiration);
        this.theSessionRepository.save(session);

        emailNotificationService.sendTwoFactorCodeNotification(
                session.getUser().getEmail(),
                session.getUser().getName(),
                newCode,
                Math.max(1, twoFactorCodeExpirationSeconds / 60)
        );

        return Map.of(
                "status", "RESENT",
                "expiresAt", newExpiration.getTime(),
                "remainingAttempts", session.getTwoFactorAttemptsLeft(),
                "maskedEmail", maskEmail(session.getUser().getEmail())
        );
    }

    public void cancelPendingTwoFactor(String challengeToken) {
        if (!StringUtils.hasText(challengeToken)) {
            return;
        }
        Session session = this.theSessionRepository.findPendingTwoFactorByToken(challengeToken);
        if (session == null || session.getRevokedAt() != null) {
            return;
        }
        session.setRevokedAt(new Date());
        this.theSessionRepository.save(session);
    }

    public HashMap<String, Object> oauthLogin(String firebaseIdToken)
            throws FirebaseAuthException, IOException {
        FirebaseToken firebaseToken = this.firebaseAuthService.verifyIdToken(firebaseIdToken);
        String provider = this.firebaseAuthService.getProvider(firebaseToken);
        if (!isAllowedOAuthProvider(provider)) {
            throw new IllegalArgumentException("Proveedor OAuth no permitido: " + provider);
        }

        User user = upsertOAuthUser(firebaseToken, provider);
        if (user == null) {
            return null;
        }

        String token = this.theJwtService.generateToken(user);
        createSession(user, token, provider);

        HashMap<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", sanitizeUser(user));
        response.put("roles", getRolesByUserId(user.getId()));
        return response;
    }

    public Map<String, Object> getUserPayloadFromToken(String token) {
        User user = this.theJwtService.getUserFromToken(token);
        if (user == null || !this.theUserRepository.existsById(user.getId())) {
            return null;
        }

        User currentUser = this.theUserRepository.findById(user.getId()).orElse(null);
        if (currentUser == null) {
            return null;
        }

        Session activeSession = this.theSessionRepository.findActiveByToken(token);
        if (activeSession == null || Boolean.FALSE.equals(currentUser.getActive())) {
            return null;
        }

        HashMap<String, Object> payload = new HashMap<>();
        payload.put("user", sanitizeUser(currentUser));
        payload.put("roles", getRolesByUserId(currentUser.getId()));
        return payload;
    }

    public boolean logout(String token) {
        Session session = this.theSessionRepository.findActiveByToken(token);
        if (session == null) {
            return false;
        }
        session.setRevokedAt(new Date());
        this.theSessionRepository.save(session);
        return true;
    }

    private User upsertOAuthUser(FirebaseToken firebaseToken, String provider) {
        String firebaseUid = firebaseToken.getUid();
        String email = firebaseToken.getEmail();
        if (!StringUtils.hasText(firebaseUid) || !StringUtils.hasText(email)) {
            return null;
        }

        User user = this.theUserRepository.getUserByFirebaseUid(firebaseUid);
        if (user == null) {
            user = this.theUserRepository.getUserByEmail(email);
        }

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setName(firebaseToken.getName());
            user.setActive(true);
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            return null;
        }

        user.setFirebaseUid(firebaseUid);
        user.setAuthProvider(provider);
        user.setEmailVerified(Boolean.TRUE.equals(firebaseToken.getClaims().get("email_verified")));
        user.setLastLoginAt(new Date());

        User saved = this.theUserRepository.save(user);
        assignDefaultRoleIfNeeded(saved);
        return saved;
    }

    private void assignDefaultRoleIfNeeded(User user) {
        Role ciudadano = this.theRoleRepository.findByName("CIUDADANO");
        if (ciudadano == null) {
            return;
        }

        List<UserRole> existingRoles = this.theUserRoleRepository.getRolesByUser(user.getId());
        boolean alreadyHasRole = existingRoles.stream()
                .anyMatch(ur -> ur.getRole() != null && ciudadano.getId().equals(ur.getRole().getId()));

        if (!alreadyHasRole) {
            this.theUserRoleRepository.save(new UserRole(user, ciudadano));
        }
    }

    private void createSession(User user, String token, String provider) {
        Session session = new Session();
        session.setToken(token);
        session.setJti(this.theJwtService.getTokenId(token));
        session.setExpiration(new Date(System.currentTimeMillis() + jwtExpiration));
        session.setProvider(provider);
        session.setUser(user);
        this.theSessionRepository.save(session);
    }

    private Session createPendingTwoFactorSession(User user, String provider) {
        String challengeToken = "2fa_" + UUID.randomUUID();
        String code = generateSixDigitCode();
        Date expiration = new Date(System.currentTimeMillis() + (twoFactorCodeExpirationSeconds * 1000));

        Session session = new Session();
        session.setToken(challengeToken);
        session.setCode2FA(code);
        session.setTwoFactorAttemptsLeft(twoFactorMaxAttempts);
        session.setExpiration(expiration);
        session.setProvider(provider);
        session.setUser(user);
        this.theSessionRepository.save(session);

        emailNotificationService.sendTwoFactorCodeNotification(
                user.getEmail(),
                user.getName(),
                code,
                Math.max(1, twoFactorCodeExpirationSeconds / 60)
        );
        return session;
    }

    private String generateSixDigitCode() {
        int randomValue = new Random().nextInt(900000) + 100000;
        return String.valueOf(randomValue);
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***@***";
        }
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];

        if (!StringUtils.hasText(localPart)) {
            localPart = "u";
        }

        String maskedLocal = localPart.length() <= 2
                ? localPart.charAt(0) + "***"
                : localPart.substring(0, 2) + "***";

        int dotIndex = domainPart.lastIndexOf('.');
        String suffix = dotIndex >= 0 ? domainPart.substring(dotIndex) : "";
        return maskedLocal + "@***" + suffix;
    }

    private boolean isAllowedOAuthProvider(String provider) {
        if (!StringUtils.hasText(provider)) {
            return false;
        }

        Set<String> allowed = Stream.of(allowedOAuthProviders.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        return allowed.contains(provider);
    }

    private List<String> getRolesByUserId(String userId) {
        List<UserRole> userRoles = this.theUserRoleRepository.getRolesByUser(userId);
        List<String> roleNames = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            if (userRole.getRole() != null && StringUtils.hasText(userRole.getRole().getName())) {
                roleNames.add(userRole.getRole().getName());
            }
        }
        return roleNames;
    }

    private Map<String, Object> sanitizeUser(User user) {
        HashMap<String, Object> userPayload = new HashMap<>();
        userPayload.put("id", user.getId());
        userPayload.put("name", user.getName());
        userPayload.put("email", user.getEmail());
        userPayload.put("firebaseUid", user.getFirebaseUid());
        userPayload.put("authProvider", user.getAuthProvider());
        userPayload.put("emailVerified", user.getEmailVerified());
        userPayload.put("active", user.getActive());
        userPayload.put("lastLoginAt", user.getLastLoginAt());
        return userPayload;
    }
}
