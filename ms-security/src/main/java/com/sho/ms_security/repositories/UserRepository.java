package com.sho.ms_security.repositories;

import com.sho.ms_security.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    @Query("{'email': ?0}")
    User getUserByEmail(String email);

    // Usado por AuthService (retorna Optional para manejo seguro de null)
    Optional<User> findByEmail(String email);

    // Usado por AuthService para verificar duplicados en registro
    boolean existsByEmail(String email);

    // HU-ENTR-1-002: Búsqueda de usuarios por nombre o email
    @Query("{ $or: [ {'name': {$regex: ?0, $options: 'i'}}, {'email': {$regex: ?0, $options: 'i'}} ] }")
    List<User> searchByNameOrEmail(String query);
}
