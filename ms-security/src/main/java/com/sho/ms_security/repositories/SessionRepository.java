package com.sho.ms_security.repositories;

import com.sho.ms_security.models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SessionRepository extends MongoRepository<Session, String> {
	@Query("{'token': ?0, 'revokedAt': null}")
	Session findActiveByToken(String token);
}
