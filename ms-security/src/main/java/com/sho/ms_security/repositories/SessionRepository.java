package com.sho.ms_security.repositories;

import com.sho.ms_security.models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface SessionRepository extends MongoRepository<Session, String> {
	@Query("{'token': ?0, 'revokedAt': null}")
	Session findActiveByToken(String token);

	@Query("{'token': ?0, 'revokedAt': null, 'code2FA': {$ne: null}}")
	Session findPendingTwoFactorByToken(String token);
}
