package com.sho.ms_security.services;

import com.sho.ms_security.models.Permission;
import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.RolePermission;
import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.repositories.PermissionRepository;
import com.sho.ms_security.repositories.RolePermissionRepository;
import com.sho.ms_security.repositories.RoleRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {

    @Autowired
    private RolePermissionRepository theRolePermissionRepository;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private PermissionRepository thePermissionRepository;

    // HU-ENTR-1-003: Para notificar a usuarios afectados
    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * HU-ENTR-1-003: Asigna un permiso a un rol.
     * El cambio se ve reflejado inmediatamente para todos los usuarios con ese rol.
     */
    public boolean addRolePermission(String roleId, String permissionId) {
        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        Permission permission = this.thePermissionRepository.findById(permissionId).orElse(null);
        if (role != null && permission != null) {
            RolePermission newRolePermission = new RolePermission(role, permission);
            this.theRolePermissionRepository.save(newRolePermission);
            // HU-ENTR-1-003: Notificar a todos los usuarios con este rol
            notifyUsersOfPermissionChange(roleId, role.getName());
            return true;
        }
        return false;
    }

    /**
     * HU-ENTR-1-003: Quita un permiso de un rol.
     * El cambio se ve reflejado inmediatamente para todos los usuarios con ese rol.
     */
    public boolean removeRolePermission(String rolePermissionId) {
        RolePermission rolePermission = this.theRolePermissionRepository
                .findById(rolePermissionId).orElse(null);
        if (rolePermission != null) {
            String roleId = rolePermission.getRole().getId();
            String roleName = rolePermission.getRole().getName();
            this.theRolePermissionRepository.delete(rolePermission);
            // HU-ENTR-1-003: Notificar a todos los usuarios con este rol
            notifyUsersOfPermissionChange(roleId, roleName);
            return true;
        }
        return false;
    }

    // HU-ENTR-1-003: Obtener permisos de un rol específico
    // Usamos findAll() + filtro en Java para evitar problemas con @DBRef en Spring Data MongoDB
    public List<RolePermission> getPermissionsByRole(String roleId) {
        return this.theRolePermissionRepository.findAll()
                .stream()
                .filter(rp -> rp.getRole() != null && roleId.equals(rp.getRole().getId()))
                .collect(Collectors.toList());
    }

    // Notifica a todos los usuarios que tienen el rol modificado
    private void notifyUsersOfPermissionChange(String roleId, String roleName) {
        List<UserRole> userRoles = this.theUserRoleRepository.findAll()
                .stream()
                .filter(ur -> ur.getRole() != null && roleId.equals(ur.getRole().getId()))
                .collect(Collectors.toList());
        for (UserRole ur : userRoles) {
            if (ur.getUser() != null) {
                emailNotificationService.sendPermissionChangeNotification(
                        ur.getUser().getEmail(),
                        ur.getUser().getName(),
                        roleName
                );
            }
        }
    }
}
