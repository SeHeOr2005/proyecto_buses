package com.sho.ms_security.controllers;

import com.sho.ms_security.models.RolePermission;
import com.sho.ms_security.services.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/role-permission")
public class RolePermissionController {

    @Autowired
    private RolePermissionService theService;

    // Obtener todos los role-permissions en una llamada (evita N+1 desde el frontend)
    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(this.theService.getAll());
    }

    // HU-ENTR-1-003: Consultar permisos específicos de un rol
    @GetMapping("role/{roleId}")
    public ResponseEntity<?> getPermissionsByRole(@PathVariable String roleId) {
        List<RolePermission> permissions = this.theService.getPermissionsByRole(roleId);
        return ResponseEntity.ok(permissions);
    }

    // HU-ENTR-1-003: Asignar permiso a un rol (se ve reflejado inmediatamente)
    @PostMapping("role/{role_id}/permission/{permission_id}")
    public ResponseEntity<Map<String, String>> addRolePermission(
            @PathVariable String role_id,
            @PathVariable String permission_id) {
        boolean response = this.theService.addRolePermission(role_id, permission_id);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Permiso asignado exitosamente al rol"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Rol o Permiso no encontrado"));
    }

    // HU-ENTR-1-003: Quitar permiso de un rol (se ve reflejado inmediatamente)
    @DeleteMapping("{role_permission_id}")
    public ResponseEntity<Map<String, String>> removeRolePermission(
            @PathVariable String role_permission_id) {
        boolean response = this.theService.removeRolePermission(role_permission_id);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Permiso removido exitosamente del rol"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Relación rol-permiso no encontrada"));
    }
}
