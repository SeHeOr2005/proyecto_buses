package com.sho.ms_security.repositories;

import com.sho.ms_security.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {

    @Query("{'email': ?0}")
    User getUserByEmail(String email);

    @Query("{'firebaseUid': ?0}")
    User getUserByFirebaseUid(String firebaseUid);

    // HU-ENTR-1-002: Búsqueda de usuarios por nombre o email
    @Query("{ $or: [ {'name': {$regex: ?0, $options: 'i'}}, {'email': {$regex: ?0, $options: 'i'}} ] }")
    List<User> searchByNameOrEmail(String query);
}
