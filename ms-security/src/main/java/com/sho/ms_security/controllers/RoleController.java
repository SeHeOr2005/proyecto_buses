package com.sho.ms_security.controllers;

import com.sho.ms_security.models.Role;
import com.sho.ms_security.services.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/roles")
public class RoleController {

    @Autowired
    private RoleService theRoleService;

    @GetMapping("")
    public List<Role> find() {
        return this.theRoleService.find();
    }

    @GetMapping("{id}")
    public Role findById(@PathVariable String id) {
        return this.theRoleService.findById(id);
    }

    @PostMapping
    public Role create(@RequestBody Role newRole) {
        return this.theRoleService.create(newRole);
    }

    @PutMapping("{id}")
    public Role update(@PathVariable String id, @RequestBody Role newRole) {
        return this.theRoleService.update(id, newRole);
    }

    /**
     * HU-ENTR-1-001: Al eliminar un rol, el sistema valida que no haya
     * usuarios asignados a ese rol.
     */
    @DeleteMapping("{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        boolean result = this.theRoleService.delete(id);
        if (result) {
            return ResponseEntity.ok(Map.of("message", "Rol eliminado exitosamente"));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message",
                        "No se puede eliminar el rol: tiene usuarios asignados o no existe"));
    }
}
