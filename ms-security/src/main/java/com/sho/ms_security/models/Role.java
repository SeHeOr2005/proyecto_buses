package com.sho.ms_security.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document
public class Role {
    @Id
    private String id;
    private String name;
    private String description;

    public Role() {}

    public Role(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
