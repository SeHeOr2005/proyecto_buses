package com.sho.ms_security.services;

import com.sho.ms_security.models.Profile;
import com.sho.ms_security.models.Role;
import com.sho.ms_security.models.Session;
import com.sho.ms_security.models.User;
import com.sho.ms_security.models.UserRole;
import com.sho.ms_security.repositories.ProfileRepository;
import com.sho.ms_security.repositories.RoleRepository;
import com.sho.ms_security.repositories.SessionRepository;
import com.sho.ms_security.repositories.UserRepository;
import com.sho.ms_security.repositories.UserRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
    private RoleRepository theRoleRepository;

    @Autowired
    private UserRoleRepository theUserRoleRepository;

    @Autowired
    private EncryptionService theEncryption;

    public List<User> find() {
        return this.theUserRepository.findAll();
    }

    public User findById(String id) {
        return this.theUserRepository.findById(id).orElse(null);
    }

    public User create(User newUser) {
        // Validar que el email no esté ya registrado
        User existing = this.theUserRepository.getUserByEmail(newUser.getEmail());
        if (existing != null) {
            return null;
        }
        newUser.setPassword(this.theEncryption.convertSHA256(newUser.getPassword()));
        User saved = this.theUserRepository.save(newUser);
        Role ciudadano = this.theRoleRepository.findByName("CIUDADANO");
        if (ciudadano != null) {
            this.theUserRoleRepository.save(new UserRole(saved, ciudadano));
        }
        return saved;
    }

    public User update(String id, User newUser) {
        User actualUser = this.theUserRepository.findById(id).orElse(null);
        if (actualUser != null) {
            actualUser.setName(newUser.getName());
            actualUser.setEmail(newUser.getEmail());
            actualUser.setPassword(this.theEncryption.convertSHA256(newUser.getPassword()));
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

    /**
     * Asigna el rol CIUDADANO a todos los usuarios que aún no lo tengan.
     * Útil como migración de datos para usuarios existentes.
     */
    public int assignCiudadanoToAll() {
        Role ciudadano = this.theRoleRepository.findByName("CIUDADANO");
        if (ciudadano == null) return 0;
        List<User> users = this.theUserRepository.findAll();
        int count = 0;
        for (User user : users) {
            List<com.sho.ms_security.models.UserRole> existing =
                    this.theUserRoleRepository.getRolesByUser(user.getId());
            boolean alreadyHasRole = existing.stream()
                    .anyMatch(ur -> ur.getRole() != null && ciudadano.getId().equals(ur.getRole().getId()));
            if (!alreadyHasRole) {
                this.theUserRoleRepository.save(new UserRole(user, ciudadano));
                count++;
            }
        }
        return count;
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
