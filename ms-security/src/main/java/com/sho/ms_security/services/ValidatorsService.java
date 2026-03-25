package com.sho.ms_security.services;

import com.sho.ms_security.models.*;
import com.sho.ms_security.repositories.PermissionRepository;
import com.sho.ms_security.repositories.RolePermissionRepository;
import com.sho.ms_security.repositories.SessionRepository;
import com.sho.ms_security.repositories.UserRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ValidatorsService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * HU-ENTR-1-001 / HU-ENTR-1-003: Los cambios en permisos de un rol aplican
     * inmediatamente porque esta validación consulta la BD en cada request.
     */
    public boolean validationRolePermission(HttpServletRequest request,
                                            String url,
                                            String method) {
        // Verificar que el usuario existe en la BD (invalida acceso si fue eliminado)
        String userId = getVerifiedUserId(request);
        if (userId == null) return false;

        // Normalizar URLs reemplazando IDs de MongoDB y números con "?"
        url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
        Permission thePermission = this.thePermissionRepository.getPermission(url, method);
        if (thePermission == null) return false;

        // Consulta dirigida: solo los roles de este usuario, sin cargar toda la colección
        List<UserRole> roles = this.theUserRoleRepository.getRolesByUser(userId);

        for (UserRole ur : roles) {
            Role theRole = ur.getRole();
            if (theRole != null) {
                // Consulta dirigida: busca exactamente el par (rol, permiso), sin cargar toda la colección
                RolePermission rp = this.theRolePermissionRepository
                        .getRolePermission(theRole.getId(), thePermission.getId());
                if (rp != null) return true;
            }
        }
        return false;
    }

    /**
     * Extrae el userId del JWT y verifica que el usuario todavía exista en la BD.
     * Garantiza que un usuario eliminado pierda acceso inmediatamente,
     * sin importar que su token aún no haya expirado.
     */
    private String getVerifiedUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            if (!jwtService.validateToken(token) || this.theSessionRepository.findActiveByToken(token) == null) {
                return null;
            }
            User userFromToken = jwtService.getUserFromToken(token);
            if (userFromToken != null) {
                User user = this.theUserRepository.findById(userFromToken.getId()).orElse(null);
                if (user != null && !Boolean.FALSE.equals(user.getActive())) {
                    return user.getId();
                }
            }
        }
        return null;
    }

    public User getUser(final HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            if (!jwtService.validateToken(token) || this.theSessionRepository.findActiveByToken(token) == null) {
                return null;
            }
            User userFromToken = jwtService.getUserFromToken(token);
            if (userFromToken != null) {
                User user = this.theUserRepository.findById(userFromToken.getId()).orElse(null);
                if (user != null && !Boolean.FALSE.equals(user.getActive())) {
                    return user;
                }
            }
        }
        return null;
    }
}
