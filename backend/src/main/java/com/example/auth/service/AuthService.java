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
        return userRepository.findByEmail(request.getEmail())
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()))
                .map(user -> new LoginResponse("Login exitoso", generateToken(), user.getName(), user.getEmail(), user.getPicture()))
                .orElse(null);
    }

    /**
     * Registra un nuevo usuario si el email no existe.
     *
     * @return mensaje de éxito o error
     */
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return "Ya existe una cuenta con ese email";
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setName(request.getName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPicture(null);

        userRepository.save(user);
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

    private String generateToken() {
        return "jwt-" + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }
}
