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
import java.util.Set;
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

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${oauth.allowed-providers:google.com,microsoft.com}")
    private String allowedOAuthProviders;

    public String login(User theNewUser) {
        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());
        if (theActualUser != null &&
                !Boolean.FALSE.equals(theActualUser.getActive()) &&
                StringUtils.hasText(theActualUser.getPassword()) &&
                theActualUser.getPassword().equals(
                        theEncryptionService.convertSHA256(theNewUser.getPassword()))) {
            String token = theJwtService.generateToken(theActualUser);
            createSession(theActualUser, token, "password");
            return token;
        }
        return null;
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
