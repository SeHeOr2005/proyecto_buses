package com.sho.ms_security.repositories;

import com.sho.ms_security.models.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRoleRepository extends MongoRepository<UserRole, String> {

    // HU-ENTR-1-002: Obtener roles de un usuario
    @Query("{ 'user' : { '$ref' : 'user', '$id' : { '$oid': ?0 } } }")
    List<UserRole> getRolesByUser(String userId);

    // HU-ENTR-1-001: Verificar si hay usuarios asignados a un rol (para validar eliminación)
    @Query("{ 'role' : { '$ref' : 'role', '$id' : { '$oid': ?0 } } }")
    List<UserRole> getUsersByRole(String roleId);
}
