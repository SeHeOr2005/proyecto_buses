package com.sho.ms_security.controllers;

import com.sho.ms_security.models.User;
import com.sho.ms_security.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService theUserService;

    @GetMapping("")
    public List<User> find() {
        return this.theUserService.find();
    }

    // HU-ENTR-1-002: Buscar usuarios por nombre o email
    @GetMapping("search")
    public List<User> search(@RequestParam String query) {
        return this.theUserService.searchByNameOrEmail(query);
    }

    @GetMapping("{id}")
    public User findById(@PathVariable String id) {
        return this.theUserService.findById(id);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody User newUser) {
        User created = this.theUserService.create(newUser);
        if (created == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya está registrado"));
        }
        return ResponseEntity.ok(created);
    }

    // Endpoint público para crear el primer usuario / registro sin token
    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody User newUser) {
        User created = this.theUserService.create(newUser);
        if (created == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "El correo ya está registrado"));
        }
        return ResponseEntity.ok(created);
    }

    @PutMapping("{id}")
    public User update(@PathVariable String id, @RequestBody User newUser) {
        return this.theUserService.update(id, newUser);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable String id) {
        this.theUserService.delete(id);
    }

    @PostMapping("{user_id}/profile/{profile_id}")
    public ResponseEntity<Map<String, String>> addUserProfile(
            @PathVariable String user_id,
            @PathVariable String profile_id) {
        boolean response = this.theUserService.addProfile(user_id, profile_id);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User or Profile not found"));
    }

    @DeleteMapping("{user_id}/profile/{profile_id}")
    public ResponseEntity<Map<String, String>> deleteUserProfile(
            @PathVariable String user_id,
            @PathVariable String profile_id) {
        boolean response = this.theUserService.removeProfile(user_id, profile_id);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User or Profile not found"));
    }

    @PostMapping("{userId}/session/{sessionId}")
    public ResponseEntity<Map<String, String>> addUserSession(
            @PathVariable String userId,
            @PathVariable String sessionId) {
        boolean response = this.theUserService.addSession(userId, sessionId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User or Session not found"));
    }

    @DeleteMapping("{userId}/session/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteUserSession(
            @PathVariable String userId,
            @PathVariable String sessionId) {
        boolean response = this.theUserService.removeSession(userId, sessionId);
        if (response) {
            return ResponseEntity.ok(Map.of("message", "Success"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User or Session not found"));
    }
}
