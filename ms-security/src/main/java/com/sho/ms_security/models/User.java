package com.sho.ms_security.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
public class User {
    @Id
    private String id;
    private String name;
    private String email;
    private String password;
    private String avatar;
    private String firebaseUid;
    private String previousFirebaseUid;
    private String authProvider;
    private Boolean emailVerified;
    private Boolean active = true;
    private Date lastLoginAt;
    @Transient
    private Boolean unlinkSocialAccount;
    private String resetPasswordToken;
    private Date resetPasswordTokenExpiration;

    public User() {
    }

    public User(String id, String name, String email, String password) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
