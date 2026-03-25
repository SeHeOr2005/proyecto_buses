package com.sho.ms_security.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
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
    private String firebaseUid;
    private String authProvider;
    private Boolean emailVerified;
    private Boolean active = true;
    private Date lastLoginAt;

    public User() {}

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
