package com.sho.ms_security.repositories;

import com.sho.ms_security.models.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProfileRepository extends MongoRepository<Profile, String> {
}
