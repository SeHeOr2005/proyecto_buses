package com.sho.ms_security.repositories;

import com.sho.ms_security.models.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface RoleRepository extends MongoRepository<Role, String> {

    @Query("{'name': ?0}")
    Role findByName(String name);
}
