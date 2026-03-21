package com.sho.ms_security.services;

import com.sho.ms_security.models.Profile;
import com.sho.ms_security.models.Session;
import com.sho.ms_security.models.User;
import com.sho.ms_security.repositories.ProfileRepository;
import com.sho.ms_security.repositories.SessionRepository;
import com.sho.ms_security.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private ProfileRepository theProfileRepository;

    @Autowired
    private SessionRepository theSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<User> find() {
        return this.theUserRepository.findAll();
    }

    public User findById(String id) {
        return this.theUserRepository.findById(id).orElse(null);
    }

    public User create(User newUser) {
        User existing = this.theUserRepository.getUserByEmail(newUser.getEmail());
        if (existing != null) {
            return null;
        }
        if (newUser.getPassword() != null) {
            newUser.setPassword(this.passwordEncoder.encode(newUser.getPassword()));
        }
        return this.theUserRepository.save(newUser);
    }

    public User update(String id, User newUser) {
        User actualUser = this.theUserRepository.findById(id).orElse(null);
        if (actualUser != null) {
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());
            if (newUser.getPassword() != null) {
                actualUser.setPassword(this.passwordEncoder.encode(newUser.getPassword()));
            }
            this.theUserRepository.save(actualUser);
            return actualUser;
        }
        return null;
    }

    public boolean delete(String id) {
        User theUser = this.theUserRepository.findById(id).orElse(null);
        if (theUser != null) {
            this.theUserRepository.delete(theUser);
            return true;
        }
        return false;
    }

    // HU-ENTR-1-002: Búsqueda por nombre o email
    public List<User> searchByNameOrEmail(String query) {
        return this.theUserRepository.searchByNameOrEmail(query);
    }

    public boolean addProfile(String userId, String profileId) {
        User user = this.theUserRepository.findById(userId).orElse(null);
        Profile profile = this.theProfileRepository.findById(profileId).orElse(null);
        if (user != null && profile != null) {
            profile.setUser(user);
            this.theProfileRepository.save(profile);
            return true;
        }
        return false;
    }

    public boolean removeProfile(String userId, String profileId) {
        User user = this.theUserRepository.findById(userId).orElse(null);
        Profile profile = this.theProfileRepository.findById(profileId).orElse(null);
        if (user != null && profile != null) {
            profile.setUser(null);
            this.theProfileRepository.save(profile);
            return true;
        }
        return false;
    }

    public boolean addSession(String userId, String sessionId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        Session theSession = this.theSessionRepository.findById(sessionId).orElse(null);
        if (theUser != null && theSession != null) {
            theSession.setUser(theUser);
            this.theSessionRepository.save(theSession);
            return true;
        }
        return false;
    }

    public boolean removeSession(String userId, String sessionId) {
        User theUser = this.theUserRepository.findById(userId).orElse(null);
        Session theSession = this.theSessionRepository.findById(sessionId).orElse(null);
        if (theUser != null && theSession != null) {
            theSession.setUser(null);
            this.theSessionRepository.save(theSession);
            return true;
        }
        return false;
    }
}
