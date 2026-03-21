package com.sho.ms_security.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "user")
public class User {

    @Id
    private String id;

    private String name;

    private String lastName;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String picture;

    /** "LOCAL", "GOOGLE" o "GITHUB" */
    private String provider;

    public User() {}

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /** Constructor para OAuth2 y registro local */
    public User(String email, String name, String password, String picture) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.picture = picture;
    }
}
