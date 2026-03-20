package com.sho.ms_security.services;

import com.sho.ms_security.models.*;
import com.sho.ms_security.repositories.PermissionRepository;
import com.sho.ms_security.repositories.RolePermissionRepository;
import com.sho.ms_security.repositories.UserRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ValidatorsService {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PermissionRepository thePermissionRepository;

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * HU-ENTR-1-001 / HU-ENTR-1-003: Los cambios en permisos de un rol aplican
     * inmediatamente porque esta validación consulta la BD en cada request.
     */
    public boolean validationRolePermission(HttpServletRequest request,
                                            String url,
                                            String method) {
        boolean success = false;
        User theUser = this.getUser(request);
        if (theUser != null) {
            // Normalizar URLs reemplazando IDs de MongoDB y números con "?"
            url = url.replaceAll("[0-9a-fA-F]{24}|\\d+", "?");
            Permission thePermission = this.thePermissionRepository.getPermission(url, method);

            // HU-ENTR-1-002: Los permisos se acumulan si tiene múltiples roles
            // Usamos findAll() + filtro en Java para evitar problemas con @DBRef en Spring Data MongoDB
            final String userId = theUser.getId();
            List<UserRole> roles = this.theUserRoleRepository.findAll()
                    .stream()
                    .filter(ur -> ur.getUser() != null && userId.equals(ur.getUser().getId()))
                    .collect(Collectors.toList());

            int i = 0;
            while (i < roles.size() && !success) {
                UserRole actual = roles.get(i);
                Role theRole = actual.getRole();
                if (theRole != null && thePermission != null) {
                    final String roleId = theRole.getId();
                    final String permId = thePermission.getId();
                    // HU-ENTR-1-003: Cambios en permisos aplican inmediatamente (consulta BD en cada request)
                    RolePermission theRolePermission = this.theRolePermissionRepository.findAll()
                            .stream()
                            .filter(rp -> rp.getRole() != null && roleId.equals(rp.getRole().getId())
                                    && rp.getPermission() != null && permId.equals(rp.getPermission().getId()))
                            .findFirst()
                            .orElse(null);
                    if (theRolePermission != null) {
                        success = true;
                    }
                }
                i++;
            }
        }
        return success;
    }

    public User getUser(final HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.substring(BEARER_PREFIX.length());
            User theUserFromToken = jwtService.getUserFromToken(token);
            if (theUserFromToken != null) {
                return this.theUserRepository.findById(theUserFromToken.getId()).orElse(null);
            }
        }
        return null;
    }
}
