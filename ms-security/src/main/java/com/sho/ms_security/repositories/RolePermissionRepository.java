package com.sho.ms_security.repositories;

import com.sho.ms_security.models.RolePermission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface RolePermissionRepository extends MongoRepository<RolePermission, String> {

    // HU-ENTR-1-003: Obtener permisos de un rol
    @Query("{ 'role' : { '$ref' : 'role', '$id' : { '$oid': ?0 } } }")
    List<RolePermission> getPermissionsByRole(String roleId);

    @Query("{ 'role' : { '$ref' : 'role', '$id' : { '$oid': ?0 } }, 'permission' : { '$ref' : 'permission', '$id' : { '$oid': ?1 } } }")
    RolePermission getRolePermission(String roleId, String permissionId);
}
