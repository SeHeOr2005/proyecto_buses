package com.sho.ms_security.services;

import com.sho.ms_security.models.Profile;
import com.sho.ms_security.repositories.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {

    @Autowired
    private ProfileRepository theProfileRepository;

    public List<Profile> find() {
        return this.theProfileRepository.findAll();
    }

    public Profile findById(String id) {
        return this.theProfileRepository.findById(id).orElse(null);
    }

    public Profile create(Profile newProfile) {
        return this.theProfileRepository.save(newProfile);
    }

    public Profile update(String id, Profile newProfile) {
        Profile actualProfile = this.theProfileRepository.findById(id).orElse(null);
        if (actualProfile != null) {
            actualProfile.setPhone(newProfile.getPhone());
            actualProfile.setPhoto(newProfile.getPhoto());
            this.theProfileRepository.save(actualProfile);
            return actualProfile;
        }
        return null;
    }

    public void delete(String id) {
        Profile theProfile = this.theProfileRepository.findById(id).orElse(null);
        if (theProfile != null) {
            this.theProfileRepository.delete(theProfile);
        }
    }
}
