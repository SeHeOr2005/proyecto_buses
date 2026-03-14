package com.sho.ms_security.repositories;

import com.sho.ms_security.models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SessionRepository extends MongoRepository<Session, String> {
}
