package com.sho.ms_security.services;

import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.User;
import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.repositories.RoleRepository;
import com.sho.ms_security.repositories.UserRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRoleService {

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRepository theUserRepository;

    // HU-ENTR-1-002: Notificación por email
    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * HU-ENTR-1-002: Asigna un rol a un usuario. Los permisos se acumulan
     * si el usuario ya tiene otros roles. Notifica al usuario por email.
     */
    public boolean addUserRole(String userId, String roleId) {
        User user = this.theUserRepository.findById(userId).orElse(null);
        Role role = this.theRoleRepository.findById(roleId).orElse(null);
        if (user != null && role != null) {
            UserRole theUserRole = new UserRole(user, role);
            this.theUserRoleRepository.save(theUserRole);
            // HU-ENTR-1-002: Notificar al usuario
            emailNotificationService.sendRoleChangeNotification(
                    user.getEmail(), user.getName(), role.getName(), "Asignación"
            );
            return true;
        }
        return false;
    }

    /**
     * HU-ENTR-1-002: Revoca un rol de un usuario. Notifica al usuario por email.
     */
    public boolean removeUserRole(String userRoleId) {
        UserRole userRole = this.theUserRoleRepository.findById(userRoleId).orElse(null);
        if (userRole != null) {
            String userEmail = userRole.getUser().getEmail();
            String userName = userRole.getUser().getName();
            String roleName = userRole.getRole().getName();
            this.theUserRoleRepository.delete(userRole);
            // HU-ENTR-1-002: Notificar al usuario
            emailNotificationService.sendRoleChangeNotification(
                    userEmail, userName, roleName, "Revocación"
            );
            return true;
        }
        return false;
    }

    // HU-ENTR-1-002: Obtener roles de un usuario
    public List<UserRole> getRolesByUser(String userId) {
        return this.theUserRoleRepository.getRolesByUser(userId);
    }

    // Obtener TODOS los user-roles (para carga en bulk desde el frontend)
    public List<UserRole> getAllUserRoles() {
        return this.theUserRoleRepository.findAll();
    }
}
