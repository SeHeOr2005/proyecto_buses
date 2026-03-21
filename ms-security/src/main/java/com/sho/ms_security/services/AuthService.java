package com.sho.ms_security.services;

import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.User;
import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.models.dto.LoginRequest;
import com.sho.ms_security.models.dto.LoginResponse;
import com.sho.ms_security.models.dto.RegisterRequest;
import com.sho.ms_security.repositories.RoleRepository;
import com.sho.ms_security.repositories.UserRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación unificado.
 * Maneja registro, login con email/contraseña, OAuth2 y cambio de contraseña.
 */
@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * HU-ENTR-1-008: Login con email y contraseña.
     * Retorna LoginResponse con JWT o null si las credenciales son inválidas.
     */
    public LoginResponse login(LoginRequest request) {
        String emailNorm = normalizeEmail(request.getEmail());
        return userRepository.findByEmail(emailNorm)
                .filter(user -> user.getPassword() != null
                        && passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    return new LoginResponse("Login exitoso", token,
                            user.getName(), user.getEmail(), user.getPicture());
                })
                .orElse(null);
    }

    /**
     * HU-ENTR-1-007: Registro con email y contraseña.
     * Crea usuario con BCrypt, asigna rol CIUDADANO y envía email de bienvenida.
     */
    public String register(RegisterRequest request) {
        String emailNorm = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(emailNorm)) {
            return "CONFLICT:Ya existe una cuenta con ese email";
        }

        User user = new User();
        user.setEmail(emailNorm);
        user.setName(request.getName().trim());
        user.setLastName(request.getLastName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider("LOCAL");
        user.setPicture(null);
        userRepository.save(user);

        // Asignar rol CIUDADANO automáticamente
        assignCiudadanoRole(user);

        // Notificación de bienvenida (asíncrona, no bloquea)
        emailNotificationService.sendWelcomeEmail(user.getEmail(), user.getName());

        return "Registro exitoso";
    }

    /**
     * HU-ENTR-1-004 / HU-ENTR-1-006: Crea o actualiza usuario OAuth2.
     * Si el email ya existe, actualiza name y picture.
     * Si es nuevo, crea con provider dado y asigna rol CIUDADANO.
     */
    public User findOrCreateOAuthUser(String email, String name, String picture, String provider) {
        String emailNorm = normalizeEmail(email);
        return userRepository.findByEmail(emailNorm)
                .map(existing -> {
                    existing.setName(name != null ? name : existing.getName());
                    existing.setPicture(picture);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(emailNorm);
                    newUser.setName(name);
                    newUser.setPassword(null);
                    newUser.setPicture(picture);
                    newUser.setProvider(provider);
                    userRepository.save(newUser);
                    assignCiudadanoRole(newUser);
                    return newUser;
                });
    }

    /**
     * HU-ENTR-1-007 (cambio de contraseña): Verifica contraseña actual con BCrypt,
     * valida fortaleza de la nueva y actualiza.
     */
    public String changePassword(User user, String oldPassword, String newPassword) {
        if (!"LOCAL".equals(user.getProvider())) {
            return "ERROR:Los usuarios OAuth2 no pueden cambiar contraseña desde aquí";
        }
        if (user.getPassword() == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            return "ERROR:Contraseña actual incorrecta";
        }
        if (newPassword == null || !newPassword.matches(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$")) {
            return "ERROR:La nueva contraseña no cumple los requisitos de seguridad";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "Contraseña actualizada. Debe iniciar sesión de nuevo.";
    }

    /**
     * Extrae el usuario del token JWT.
     */
    public User getUserFromToken(String token) {
        User fromToken = jwtService.getUserFromToken(token);
        if (fromToken == null) return null;
        return userRepository.findById(fromToken.getId()).orElse(null);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String normalizeEmail(String email) {
        return email != null ? email.trim().toLowerCase() : "";
    }

    private void assignCiudadanoRole(User user) {
        roleRepository.findAll().stream()
                .filter(r -> "CIUDADANO".equals(r.getName()))
                .findFirst()
                .ifPresent(role -> {
                    UserRole userRole = new UserRole(user, role);
                    userRoleRepository.save(userRole);
                });
    }
}
