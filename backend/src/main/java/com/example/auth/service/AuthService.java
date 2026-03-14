package com.example.auth.service;

import com.example.auth.model.LoginRequest;
import com.example.auth.model.LoginResponse;
import com.example.auth.model.RegisterRequest;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servicio de autenticación que usa MongoDB para persistir usuarios.
 * - Login: busca usuario por email y verifica contraseña
 * - Register: verifica si el email ya existe, si no guarda el usuario con contraseña hasheada
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        String emailNorm = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        return userRepository.findByEmail(emailNorm)
                .filter(user -> user.getPassword() != null && passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> {
                    String token = generateToken();
                    user.setSessionToken(token);
                    userRepository.save(user);
                    return new LoginResponse("Login exitoso", token, user.getName(), user.getEmail(), user.getPicture());
                })
                .orElse(null);
    }

    /**
     * Registra un nuevo usuario si el email no existe (evita usuarios duplicados).
     *
     * @return mensaje de éxito o error
     */
    public String register(RegisterRequest request) {
        String emailNorm = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(emailNorm)) {
            return "Ya existe una cuenta con ese email";
        }

        User user = new User();
        user.setEmail(emailNorm);
        user.setName(request.getName().trim());
        user.setLastName(request.getLastName() != null ? request.getLastName().trim() : null);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPicture(null);
        user.setSessionToken(null);

        userRepository.save(user);
        // TODO: enviar email de confirmación de creación de cuenta (SMTP/SendGrid)
        return "Registro exitoso";
    }

    /**
     * Crea o actualiza usuario tras login OAuth (GitHub, Google).
     * Si el email ya existe, actualiza nombre y foto. Si no, crea usuario nuevo.
     */
    public User findOrCreateOAuthUser(String email, String name, String picture) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setName(name != null ? name : user.getName());
                    user.setPicture(picture);
                    return userRepository.save(user);
                })
                .orElseGet(() -> {
                    User user = new User(email, name, null, picture);
                    userRepository.save(user);
                    return user;
                });
    }

    /**
     * Cambia la contraseña del usuario e invalida el token actual (debe volver a iniciar sesión).
     */
    public String changePassword(User user, String oldPassword, String newPassword) {
        if (user.getPassword() == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            return "Contraseña actual incorrecta";
        }
        if (newPassword == null || newPassword.length() < 8
                || !newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$")) {
            return "La nueva contraseña no cumple los requisitos (mín. 8 caracteres, mayúscula, minúscula, número, carácter especial)";
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setSessionToken(null);
        userRepository.save(user);
        return "Contraseña actualizada. Debe iniciar sesión de nuevo.";
    }

    private String generateToken() {
        return "jwt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
