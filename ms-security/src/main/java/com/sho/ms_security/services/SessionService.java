package com.sho.ms_security.services;

import com.sho.ms_security.models.Session;
import com.sho.ms_security.repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    @Autowired
    private SessionRepository theSessionRepository;

    public List<Session> find() {
        return this.theSessionRepository.findAll();
    }

    public Session findById(String id) {
        return this.theSessionRepository.findById(id).orElse(null);
    }

    public Session create(Session newSession) {
        return this.theSessionRepository.save(newSession);
    }

    public Session update(String id, Session newSession) {
        Session actualSession = this.theSessionRepository.findById(id).orElse(null);
        if (actualSession != null) {
            actualSession.setToken(newSession.getToken());
            actualSession.setExpiration(newSession.getExpiration());
            actualSession.setCode2FA(newSession.getCode2FA());
            actualSession.setUser(newSession.getUser());
            this.theSessionRepository.save(actualSession);
            return actualSession;
        }
        return null;
    }

    public void delete(String id) {
        Session theSession = this.theSessionRepository.findById(id).orElse(null);
        if (theSession != null) {
            this.theSessionRepository.delete(theSession);
        }
    }
}
