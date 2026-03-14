package com.sho.ms_security.services;

import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.repositories.RoleRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    @Autowired
    private RoleRepository theRoleRepository;

    // HU-ENTR-1-001: Para validar que no hay usuarios antes de eliminar un rol
    @Autowired
    private UserRoleRepository theUserRoleRepository;

    public List<Role> find() {
        return this.theRoleRepository.findAll();
    }

    public Role findById(String id) {
        return this.theRoleRepository.findById(id).orElse(null);
    }

    public Role create(Role newRole) {
        return this.theRoleRepository.save(newRole);
    }

    public Role update(String id, Role newRole) {
        Role actualRole = this.theRoleRepository.findById(id).orElse(null);
        if (actualRole != null) {
            actualRole.setName(newRole.getName());
            actualRole.setDescription(newRole.getDescription());
            this.theRoleRepository.save(actualRole);
            return actualRole;
        }
        return null;
    }

    /**
     * HU-ENTR-1-001: Al eliminar un rol, valida que no haya usuarios asignados.
     * @return true si se eliminó, false si tiene usuarios asignados o no existe
     */
    public boolean delete(String id) {
        Role theRole = this.theRoleRepository.findById(id).orElse(null);
        if (theRole == null) {
            return false;
        }
        List<UserRole> assignedUsers = this.theUserRoleRepository.getUsersByRole(id);
        if (!assignedUsers.isEmpty()) {
            return false; // No se puede eliminar: hay usuarios con este rol
        }
        this.theRoleRepository.delete(theRole);
        return true;
    }
}
