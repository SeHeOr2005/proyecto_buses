package com.sho.ms_security.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document
public class Session {
    @Id
    private String id;
    private String token;
    private String jti;
    private Date expiration;
    private String code2FA;
    private String provider;
    private String deviceInfo;
    private String ip;
    private Date revokedAt;

    @DBRef
    private User user;

    public Session() {}

    public Session(String id, String token, Date expiration, String code2FA, User user) {
        this.id = id;
        this.token = token;
        this.expiration = expiration;
        this.code2FA = code2FA;
        this.user = user;
    }
}
