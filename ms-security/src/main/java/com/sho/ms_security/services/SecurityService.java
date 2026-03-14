package com.sho.ms_security.services;

import com.sho.ms_security.models.User;
import com.sho.ms_security.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityService {

    @Autowired
    private UserRepository theUserRepository;

    @Autowired
    private EncryptionService theEncryptionService;

    @Autowired
    private JwtService theJwtService;

    public String login(User theNewUser) {
        User theActualUser = this.theUserRepository.getUserByEmail(theNewUser.getEmail());
        if (theActualUser != null &&
                theActualUser.getPassword().equals(
                        theEncryptionService.convertSHA256(theNewUser.getPassword()))) {
            return theJwtService.generateToken(theActualUser);
        }
        return null;
    }
}
