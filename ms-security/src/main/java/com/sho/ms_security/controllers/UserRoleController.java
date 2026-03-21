package com.sho.ms_security.controllers;

import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.services.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user-role")
public class UserRoleController {

    @Autowired
    private UserRoleService theUserRoleService;

    // Obtener TODOS los user-roles en una sola llamada (evita N+1 peticiones desde el frontend)
    @GetMapping
    public ResponseEntity<?> getAllUserRoles() {
        List<UserRole> allRoles = this.theUserRoleService.getAllUserRoles();
        return ResponseEntity.ok(allRoles);
    }

    // HU-ENTR-1-002: Ver roles de un usuario con sus roles actuales
    @GetMapping("user/{userId}")
    public ResponseEntity<?> getRolesByUser(@PathVariable String userId) {
        List<UserRole> roles = this.theUserRoleService.getRolesByUser(userId);
        return ResponseEntity.ok(roles);
    }

    // HU-ENTR-1-002: Asignar rol a usuario (soporta múltiples roles)
    @PostMapping("user/{userId}/role/{roleId}")
    public ResponseEntity<Map<String, String>> addUserRole(
            @PathVariable String userId,
            @PathVariable String roleId) {
        boolean response = this.theUserRoleService.addUserRole(userId, roleId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Rol asignado exitosamente"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuario o Rol no encontrado"));
    }

    // HU-ENTR-1-002: Revocar rol de usuario
    @DeleteMapping("{userRoleId}")
    public ResponseEntity<Map<String, String>> removeUserRole(
            @PathVariable String userRoleId) {
        boolean response = this.theUserRoleService.removeUserRole(userRoleId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Rol revocado exitosamente"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Relación usuario-rol no encontrada"));
    }
}
